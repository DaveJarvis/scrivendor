/* Copyright 2020-2021 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite;

import com.keenwrite.editors.TextDefinition;
import com.keenwrite.editors.TextEditor;
import com.keenwrite.editors.definition.DefinitionTreeItem;
import com.keenwrite.sigils.SigilOperator;

import static com.keenwrite.Constants.*;
import static com.keenwrite.events.StatusEvent.clue;

/**
 * Provides the logic for injecting variable names within the editor.
 */
public final class DefinitionNameInjector {

  /**
   * Prevent instantiation.
   */
  private DefinitionNameInjector() {
  }

  /**
   * Find a node that matches the current word and substitute the definition
   * reference.
   */
  public static void autoinsert(
    final TextEditor editor,
    final TextDefinition definitions,
    final SigilOperator operator ) {
    try {
      if( definitions.isEmpty() ) {
        clue( STATUS_DEFINITION_EMPTY );
      }
      else {
        final var indexes = editor.getCaretWord();
        final var word = editor.getText( indexes );

        if( word.isBlank() ) {
          clue( STATUS_DEFINITION_BLANK );
        }
        else {
          final var leaf = findLeaf( definitions, word );

          if( leaf == null ) {
            clue( STATUS_DEFINITION_MISSING, word );
          }
          else {
            final var entokened = operator.entoken( leaf.toPath() );
            editor.replaceText( indexes, operator.apply( entokened ) );
            definitions.expand( leaf );
          }
        }
      }
    } catch( final Exception ex ) {
      clue( STATUS_DEFINITION_BLANK, ex );
    }
  }

  /**
   * Looks for the given word, matching first by exact, next by a starts-with
   * condition with diacritics replaced, then by containment.
   *
   * @param word Match the word by: exact, beginning, containment, or other.
   */
  @SuppressWarnings( "ConstantConditions" )
  private static DefinitionTreeItem<String> findLeaf(
    final TextDefinition definition, final String word ) {
    assert word != null;

    DefinitionTreeItem<String> leaf = null;

    leaf = leaf == null ? definition.findLeafExact( word ) : leaf;
    leaf = leaf == null ? definition.findLeafStartsWith( word ) : leaf;
    leaf = leaf == null ? definition.findLeafContains( word ) : leaf;
    leaf = leaf == null ? definition.findLeafContainsNoCase( word ) : leaf;

    return leaf;
  }
}
