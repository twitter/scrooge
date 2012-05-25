package com.twitter.scrooge;

import java.util.*;

public abstract class ScroogeOption<A> {
  public abstract A get();
  public abstract boolean isDefined();

  public static <A> ScroogeOption<A> make(boolean b, A a) {
    if (b) {
      return new Some<A>(a);
    } else {
      return none();
    }
  }

  public static <A> ScroogeOption<A> none() {
    return (ScroogeOption<A>) NONE;
  }

  public static ScroogeOption<Void> NONE = new ScroogeOption<Void>() {
    public Void get() {
      throw new IllegalArgumentException();
    }

    public boolean isDefined() {
      return false;
    }
  };

  public static class Some<A> extends ScroogeOption<A> {
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
