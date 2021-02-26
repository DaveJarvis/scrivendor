/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.ui.fonts;

import com.keenwrite.io.MediaType;
import com.keenwrite.io.MediaTypeExtension;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static com.keenwrite.events.StatusEvent.clue;
import static com.keenwrite.io.MediaTypeExtension.MEDIA_UNDEFINED;
import static com.keenwrite.preview.SvgRasterizer.BROKEN_IMAGE_PLACEHOLDER;
import static com.keenwrite.preview.SvgRasterizer.rasterize;
import static java.awt.Font.BOLD;
import static javafx.embed.swing.SwingFXUtils.toFXImage;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.controlsfx.glyphfont.FontAwesome.Glyph.valueOf;

/**
 * Responsible for creating FontAwesome glyphs and graphics.
 */
public class IconFactory {

  /**
   * Singleton to prevent re-loading the TTF file.
   */
  private static final FontAwesome FONT_AWESOME = new FontAwesome();

  /**
   * Caches file type icons encountered.
   */
  private static final Map<String, Image> ICONS = new HashMap<>();

  /**
   * Prevent instantiation. Use the {@link #createGraphic(String)} method to
   * create an icon for display.
   */
  private IconFactory() {}

  /**
   * Create a {@link Node} representation for the given icon name.
   *
   * @param icon Name of icon to convert to a UI object (case-insensitive).
   * @return A UI object suitable for display.
   */
  public static Node createGraphic( final String icon ) {
    assert icon != null;

    // Return a label glyph.
    return icon.isEmpty()
      ? new Glyph()
      : createGlyph( icon );
  }

  /**
   * Create a {@link Node} representation for the given FontAwesome glyph.
   *
   * @param glyph The glyph to convert to a {@link Node}.
   * @return The given glyph as a text label.
   */
  public static Node createGraphic( final FontAwesome.Glyph glyph ) {
    return FONT_AWESOME.create( glyph );
  }

  /**
   * Creates a suitable {@link Node} icon representation for the given file.
   * This will first look up the {@link MediaType} before matching based on
   * the file name extension.
   *
   * @param file The file to represent graphically.
   * @return An icon representation for the given file.
   */
  public static ImageView createFileIcon(
    final File file, final BasicFileAttributes attrs ) {
    final var filename = file.getName();
    String extension;

    if( "..".equals( filename ) ) {
      extension = "folder-up";
    }
    else if( attrs.isDirectory() ) {
      extension = "folder";
    }
    else if( attrs.isSymbolicLink() ) {
      extension = "folder-link";
    }
    else {
      final var mediaType = MediaType.valueFrom( file );
      final var mte = MediaTypeExtension.valueFrom( mediaType );

      // if the file extension is not known to the app, try loading an icon
      // that corresponds to the extension directly.
      extension = mte == MEDIA_UNDEFINED
        ? getExtension( filename )
        : mte.getExtension().toLowerCase();
    }

    // Each cell in the table must have a distinct parent, so the image views
    // cannot be reused. The underlying buffered image can be cached, though.
    final var image =
      ICONS.computeIfAbsent( extension, IconFactory::createImageView );
    final var imageView = new ImageView();
    imageView.setPreserveRatio( true );
    imageView.setFitWidth( 42 );
    imageView.setImage( image );

    return imageView;
  }

  private static javafx.scene.image.Image createImageView(
    final String extension ) {
    try( final var icon = open( "icons/" + extension + ".svg" ) ) {
      return rasterize( icon );
    } catch( final Exception ex ) {
      clue( ex );

      // If the extension was unknown, fall back to a blank icon.
      final var image = createImageView( "blank" );

      // If the blank icon cannot be found, fall back to a broken image.
      if( image == null ) {
        return toFXImage( BROKEN_IMAGE_PLACEHOLDER, null );
      }
    }

    return null;
  }

  private static InputStream open( final String resource ) {
    return IconFactory.class.getResourceAsStream( resource );
  }

  public static Font getIconFont( final int size ) {
    return new Font( FONT_AWESOME.getName(), BOLD, size );
  }

  private static Node createGlyph( final String icon ) {
    return createGraphic( valueOf( icon.toUpperCase() ) );
  }
}