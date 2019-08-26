package com.tdim.qas;

public final class ASConstants {
    public enum InputFormat {
        None, SBS, TB;
        @Override
        public String toString() {
            switch(this) {
                case None:  return "2D";
                case SBS:   return "Side-by-Side";
                case TB:    return "Top/Bottom";
                default:
                    throw new IllegalArgumentException();
            }
        }
    }
}
