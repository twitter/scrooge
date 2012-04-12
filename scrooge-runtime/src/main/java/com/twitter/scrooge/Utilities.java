package com.twitter.scrooge;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Map;

public class Utilities {
  static class Tuple<A, B> {
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

  <T> List<T> makeList(T... elements) {
    return Arrays.asList(elements);
  }

  <A, B> Map<A, B> makeMap(Tuple<A, B>... elements) {
    return null;
  }

  <T> Set<T> makeSet(T... elements) {
    return null;
  }

  <A, B> Tuple<A, B> makeTuple(A a, B b) {
    return new Tuple<A, B>(a, b);
  }

  public static abstract class Option<A> {
    public abstract A get();
    public abstract boolean isDefined();
    
    public static Option<? extends Void> NONE = new Option<Void>() {
      public Void get() {
        throw new IllegalArgumentException();
      }
  
      public boolean isDefined() {
        return false;
      }
    };

    public static class Some<A> extends Option<A> {
      private final A a;

      public Some(A a) {
        this.a = a;
      }

      public A get() {
        return a;
      }

      public boolean isDefined() {
        return true;
      }
    }
  }
}