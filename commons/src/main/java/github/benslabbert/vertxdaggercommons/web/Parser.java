/* Licensed under Apache-2.0 2024. */
package github.benslabbert.vertxdaggercommons.web;

public interface Parser<T> {

  T parse(String value);

  T parse(String value, T defaultValue);
}
