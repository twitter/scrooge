package com.twitter.scrooge;

import java.util.*;

public class Utilities {
  public static class Tuple<A, B> {
    private A a;
    private B b;

    public Tuple(A a, B b) {
      this.a = a;
      this.b = b;
    }

    A getFirst() {
      return this.a;
    }

    B getSecond() {
      return this.b;
    }
  }

  public static <T> List<T> makeList(T... elements) {
    return Arrays.asList(elements);
  }

  public static <A, B> Map<A, B> makeMap(Tuple<A, B>... elements) {
    Map<A, B> map = new HashMap<A, B>(elements.length * 2 / 3);
    for (Tuple<A, B> element : elements) {
      map.put(element.getFirst(), element.getSecond());
    }
    return map;
  }

  public static <T> Set<T> makeSet(T... elements) {
    Set<T> set = new HashSet<T>(elements.length * 2 / 3);
    for (T element : elements) {
      set.add(element);
    }
    return set;
  }

  public static <T extends Enum<T>> Set<T> makeEnumSet(Class<T> klass) {
    return EnumSet.noneOf(klass);
  }

  public static <T extends Enum<T>> Set<T> makeEnumSet(T first, T... elements) {
    return EnumSet.of(first, elements);
  }

  public static <A, B> Tuple<A, B> makeTuple(A a, B b) {
    return new Tuple<A, B>(a, b);
  }
}
