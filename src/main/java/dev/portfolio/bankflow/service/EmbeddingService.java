package dev.portfolio.bankflow.service;
public interface EmbeddingService {
    double[] embed(String text);
    String provider();
    static String vectorLiteral(double[] values) {
        StringBuilder s = new StringBuilder("[");
        for (int i=0;i<values.length;i++) {
            if (i>0) s.append(',');
            s.append(Float.toString((float)values[i]));
        }
        return s.append(']').toString();
    }
}
