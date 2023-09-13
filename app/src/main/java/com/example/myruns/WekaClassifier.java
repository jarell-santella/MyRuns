package com.example.myruns;

class WekaClassifier {

    public static double classify(Object[] i)
            throws Exception {

        double p = Double.NaN;
        p = WekaClassifier.N524c043f43(i);
        return p;
    }
    static double N524c043f43(Object []i) {
        double p = Double.NaN;
        if (i[18] == null) {
            p = 0;
        } else if (((Double) i[18]).doubleValue() <= 1.671642) {
            p = 0;
        } else if (((Double) i[18]).doubleValue() > 1.671642) {
            p = WekaClassifier.N3b291d8344(i);
        }
        return p;
    }
    static double N3b291d8344(Object []i) {
        double p = Double.NaN;
        if (i[64] == null) {
            p = 1;
        } else if (((Double) i[64]).doubleValue() <= 12.478857) {
            p = WekaClassifier.Ndae90f845(i);
        } else if (((Double) i[64]).doubleValue() > 12.478857) {
            p = 2;
        }
        return p;
    }
    static double Ndae90f845(Object []i) {
        double p = Double.NaN;
        if (i[0] == null) {
            p = 1;
        } else if (((Double) i[0]).doubleValue() <= 419.701032) {
            p = 1;
        } else if (((Double) i[0]).doubleValue() > 419.701032) {
            p = WekaClassifier.N79be3a2746(i);
        }
        return p;
    }
    static double N79be3a2746(Object []i) {
        double p = Double.NaN;
        if (i[20] == null) {
            p = 1;
        } else if (((Double) i[20]).doubleValue() <= 2.118763) {
            p = 1;
        } else if (((Double) i[20]).doubleValue() > 2.118763) {
            p = 2;
        }
        return p;
    }
}