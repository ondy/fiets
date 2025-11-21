package fiets.util;

import jodd.jerry.Jerry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper for creating {@link Jerry} instances across Jodd versions.
 */
public final class JerryFactory {
  private static final Method JERRY_STRING_METHOD = findJerryStringMethod();
  private static final Method JERRY_NOARG_METHOD = findJerryNoArgMethod();
  private static final Method JERRY_PARSE_METHOD = findParseMethod(JERRY_NOARG_METHOD);

  private JerryFactory() {
  }

  public static Jerry parse(String html) {
    try {
      if (JERRY_STRING_METHOD != null) {
        return (Jerry) JERRY_STRING_METHOD.invoke(null, html);
      }
      if (JERRY_NOARG_METHOD != null && JERRY_PARSE_METHOD != null) {
        Object parser = JERRY_NOARG_METHOD.invoke(null);
        return (Jerry) JERRY_PARSE_METHOD.invoke(parser, html);
      }
      throw new IllegalStateException("No compatible Jerry parsing method found.");
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Could not create Jerry instance via reflection.", e);
    }
  }

  private static Method findJerryStringMethod() {
    try {
      return Jerry.class.getMethod("jerry", String.class);
    } catch (NoSuchMethodException ignored) {
      return null;
    }
  }

  private static Method findJerryNoArgMethod() {
    try {
      return Jerry.class.getMethod("jerry");
    } catch (NoSuchMethodException ignored) {
      return null;
    }
  }

  private static Method findParseMethod(Method jerryNoArgMethod) {
    if (jerryNoArgMethod == null) {
      return null;
    }
    Class<?> parserClass = jerryNoArgMethod.getReturnType();
    try {
      return parserClass.getMethod("parse", String.class);
    } catch (NoSuchMethodException ignored) {
      return null;
    }
  }
}
