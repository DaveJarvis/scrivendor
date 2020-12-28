/* Copyright 2020 White Magic Software, Ltd. -- All rights reserved. */
package com.keenwrite.preferences;

import javafx.beans.property.SimpleObjectProperty;

import java.io.File;

public class FileProperty extends SimpleObjectProperty<File> {
  public FileProperty( final File file ) {
    super( file );
  }

  public void setValue( final String filename ) {
    setValue( new File( filename ) );
  }
}