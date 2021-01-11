/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.processors.markdown.extensions.tex;

import com.keenwrite.ExportFormat;
import com.keenwrite.processors.Processor;
import com.keenwrite.processors.ProcessorContext;
import com.keenwrite.processors.markdown.extensions.HtmlRendererAdapter;
import com.keenwrite.processors.markdown.extensions.tex.TexNodeRenderer.Factory;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import org.jetbrains.annotations.NotNull;

import static com.vladsch.flexmark.parser.Parser.ParserExtension;

/**
 * Responsible for wrapping delimited TeX code in Markdown into an XML element
 * that the HTML renderer can handle. For example, {@code $E=mc^2$} becomes
 * {@code <tex>E=mc^2</tex>} when passed to HTML renderer. The HTML renderer
 * is responsible for converting the TeX code for display. This avoids inserting
 * SVG code into the Markdown document, which the parser would then have to
 * iterate---a <em>very</em> wasteful operation that impacts front-end
 * performance.
 */
public class TeXExtension extends HtmlRendererAdapter
  implements ParserExtension {

  /**
   * Controls how the node renderer produces TeX code within HTML output.
   */
  private final ExportFormat mExportFormat;

  private final Processor<String> mProcessor;

  private TeXExtension(
    final ProcessorContext context, final Processor<String> processor ) {
    mExportFormat = context.getExportFormat();
    mProcessor = processor;
  }

  /**
   * Creates an extension capable of handling delimited TeX code in Markdown.
   *
   * @return The new {@link TeXExtension}, never {@code null}.
   */
  public static TeXExtension create(
    final ProcessorContext context, final Processor<String> processor ) {
    return new TeXExtension( context, processor );
  }

  /**
   * Adds the TeX extension for HTML document export types.
   *
   * @param builder      The document builder.
   * @param rendererType Indicates the document type to be built.
   */
  @Override
  public void extend( @NotNull final HtmlRenderer.Builder builder,
                      @NotNull final String rendererType ) {
    if( "HTML".equalsIgnoreCase( rendererType ) ) {
      builder.nodeRendererFactory( new Factory( mExportFormat, mProcessor ) );
    }
  }

  @Override
  public void extend( final Parser.Builder builder ) {
    builder.customDelimiterProcessor( new TeXInlineDelimiterProcessor() );
  }

  @Override
  public void parserOptions( final MutableDataHolder options ) {
  }
}
