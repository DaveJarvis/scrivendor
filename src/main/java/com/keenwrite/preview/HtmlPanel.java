/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.preview;

import com.keenwrite.ui.adapters.DocumentAdapter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.render.Box;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;
import org.xhtmlrenderer.swing.BasicPanel;
import org.xhtmlrenderer.swing.FSMouseListener;
import org.xhtmlrenderer.swing.HoverListener;
import org.xhtmlrenderer.swing.LinkListener;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.net.URI;

import static com.keenwrite.events.FileOpenEvent.fireFileOpenEvent;
import static com.keenwrite.events.DocumentChangedEvent.fireDocumentChangedEvent;
import static com.keenwrite.events.StatusEvent.clue;
import static com.keenwrite.util.ProtocolScheme.getProtocol;
import static java.awt.Desktop.Action.BROWSE;
import static java.awt.Desktop.getDesktop;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.SwingUtilities.isEventDispatchThread;
import static org.jsoup.Jsoup.parse;

/**
 * Responsible for configuring FlyingSaucer's {@link XHTMLPanel}.
 */
public final class HtmlPanel extends XHTMLPanel {

  /**
   * Suppresses scroll attempts until after the document has loaded.
   */
  private static final class DocumentEventHandler extends DocumentAdapter {
    private final BooleanProperty mReadyProperty = new SimpleBooleanProperty();

    @Override
    public void documentStarted() {
      mReadyProperty.setValue( FALSE );
    }

    @Override
    public void documentLoaded() {
      mReadyProperty.setValue( TRUE );
    }
  }

  /**
   * Ensures that the preview panel fills its container's area completely.
   */
  private final class ComponentEventHandler extends ComponentAdapter {
    /**
     * Invoked when the component's size changes.
     */
    public void componentResized( final ComponentEvent e ) {
      setPreferredSize( e.getComponent().getPreferredSize() );
    }
  }

  /**
   * Responsible for opening hyperlinks. External hyperlinks are opened in
   * the system's default browser; local file system links are opened in the
   * editor.
   */
  private static final class HyperlinkListener extends LinkListener {
    @Override
    public void linkClicked( final BasicPanel panel, final String uri ) {
      try {
        switch( getProtocol( uri ) ) {
          case HTTP -> {
            final var desktop = getDesktop();

            if( desktop.isSupported( BROWSE ) ) {
              desktop.browse( new URI( uri ) );
            }
          }
          case FILE -> fireFileOpenEvent( new URI( uri ) );
        }
      } catch( final Exception ex ) {
        clue( ex );
      }
    }
  }

  private static final DomConverter CONVERTER = new DomConverter();
  private static final XhtmlNamespaceHandler XNH = new XhtmlNamespaceHandler();

  public HtmlPanel() {
    addDocumentListener( new DocumentEventHandler() );
    removeMouseTrackingListeners();
    addMouseTrackingListener( new HyperlinkListener() );
    addComponentListener( new ComponentEventHandler() );
  }

  /**
   * Updates the document model displayed by the renderer. Effectively, this
   * updates the HTML document to provide new content.
   *
   * @param html    A complete HTML5 document, including doctype.
   * @param baseUri URI to use for finding relative files, such as images.
   */
  public void render( final String html, final String baseUri ) {
    final var soup = parse( html );
    final var doc = CONVERTER.fromJsoup( soup );
    final Runnable renderDocument = () -> setDocument( doc, baseUri, XNH );

    // Access to a Swing component must occur from the Event Dispatch
    // Thread (EDT) according to Swing threading restrictions. Setting a new
    // document invokes a Swing repaint operation.
    if( isEventDispatchThread() ) {
      renderDocument.run();
    }
    else {
      invokeLater( renderDocument );
    }

    // When the text changes, let subscribers know. This allows for text
    // analysis to occur on a separate thread.
    fireDocumentChangedEvent( soup );
  }

  /**
   * Delegates to the {@link SharedContext}.
   *
   * @param id The HTML element identifier to retrieve in {@link Box} form.
   * @return The {@link Box} that corresponds to the given element ID, or
   * {@code null} if none found.
   */
  public Box getBoxById( final String id ) {
    return getSharedContext().getBoxById( id );
  }

  /**
   * Suppress scrolling to the top on updates.
   */
  @Override
  public void resetScrollPosition() {
  }

  /**
   * The default mouse click listener attempts navigation within the preview
   * panel. We want to usurp that behaviour to open the link in a
   * platform-specific browser.
   */
  private void removeMouseTrackingListeners() {
    for( final var listener : getMouseTrackingListeners() ) {
      if( !(listener instanceof HoverListener) ) {
        removeMouseTrackingListener( (FSMouseListener) listener );
      }
    }
  }
}
