package uk.gov.hmcts.reform.bulkscanprocessor.helper;

import java.util.Comparator;

public class ToStringComparator<T> implements Comparator<T> {
    @Override
    public int compare(T o1, T o2) {
        if (o1 == o2) {
            return 0;
        }

        if (o1 == null) {
            return -1;
        }

        if (o2 == null) {
            return 1;
        }

        return o1.toString().compareToIgnoreCase(o2.toString());
    }
}
