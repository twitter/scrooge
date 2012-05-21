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
    Map<A, B> map = new HashMap<A, B>();
    for (Tuple<A, B> element : elements) {
      map.put(element.getFirst(), element.getSecond());
    }
    return map;
  }

  public static <T> Set<T> makeSet(T... elements) {
    Set<T> set = new HashSet<T>();
    for (T element : elements) {
      set.add(element);
    }
    return set;
  }

  public static <A, B> Tuple<A, B> makeTuple(A a, B b) {
    return new Tuple<A, B>(a, b);
  }

  public static abstract class Option<A> {
    public abstract A get();
    public abstract boolean isDefined();

    public static <A> Option<A> make(boolean b, A a) {
      if (b) {
        return new Some<A>(a);
      } else {
        return none();
      }
    }

    public static <A> Option<A> none() {
      return (Option<A>) NONE;
    }

    public static Option<Void> NONE = new Option<Void>() {
      public Void get() {
        throw new IllegalArgumentException();
      }
  
      public boolean isDefined() {
        return false;
      }
    };

    public static class Some<A> extends Option<A> {
      final A a;

      public Some(A a) {
        this.a = a;
      }

      public A get() {
        return a;
      }

      public boolean isDefined() {
        return true;
      }

      public boolean equals(Object other) {
        if (!(other instanceof Some)) return false;
        Some<A> that = (Some<A>) other;
        return this.a.equals(that.a);
      }

      public String toString() {
        return "Some(" + this.a.toString() + ")";
      }
    }
  }
}