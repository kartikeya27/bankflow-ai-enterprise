package dev.portfolio.bankflow.service;
import java.util.*;
/** Repeatable token-hashing embedding for OFFLINE demos. Not semantically comparable to Titan vectors. */
public class LocalEmbeddingService implements EmbeddingService {
    @Override public String provider() { return "LOCAL_HASH_1024"; }
    @Override public double[] embed(String text) {
        double[] v = new double[1024];
        for (String token : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (token.isBlank()) continue;
            int h = token.hashCode();
            int index = Math.floorMod(h, v.length);
            v[index] += (h & 1) == 0 ? 1 : -1;
        }
        double norm=0;
        for (double x : v) norm+=x*x;
        norm=Math.sqrt(norm);
        if(norm==0) v[0]=1;
        else for(int i=0;i<v.length;i++)v[i]/=norm;
        return v;
    }
}
