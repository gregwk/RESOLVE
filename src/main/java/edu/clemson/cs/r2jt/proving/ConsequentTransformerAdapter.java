package edu.clemson.cs.r2jt.proving;

import java.util.Iterator;

public class ConsequentTransformerAdapter implements ConsequentTransformer {

    private static final Mapper<ImmutableConjuncts, Consequent> MAP_TO_CONSEQUENTS =
            new ConsequentCastMapper();

    private final ConjunctsTransformer myTransformer;

    public ConsequentTransformerAdapter(ConjunctsTransformer t) {
        myTransformer = t;
    }

    public Iterator<Consequent> transform(Consequent original) {
        return new LazyMappingIterator<ImmutableConjuncts, Consequent>(
                myTransformer.transform(original), MAP_TO_CONSEQUENTS);
    }

    public String toString() {
        return myTransformer.toString();
    }
}
