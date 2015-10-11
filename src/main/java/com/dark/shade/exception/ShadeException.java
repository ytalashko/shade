package com.dark.shade.exception;

/**
 * Shade exception.
 */
public class ShadeException extends RuntimeException {

  public ShadeException() {
  }

  public ShadeException(String message) {
    super(message);
  }

  public ShadeException(String message, Throwable cause) {
    super(message, cause);
  }
}
