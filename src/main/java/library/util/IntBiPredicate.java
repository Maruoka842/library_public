package library.util;

@FunctionalInterface
public interface IntBiPredicate {
    boolean test(int x, int y);
}