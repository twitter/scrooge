package com.twitter.scrooge;

import java.util.*;

public abstract class Option<A> {
  public abstract A get();
  public abstract boolean isDefined();

  public static <A> Option<A> make(boolean b, A a) {
    if (b) {
      return new Some<A>(a);
    } else {
      return none();
    }
  }

  @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
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
