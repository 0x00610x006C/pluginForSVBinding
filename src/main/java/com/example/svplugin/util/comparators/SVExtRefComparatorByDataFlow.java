package com.example.svplugin.util.comparators;

import com.sclTools.ch.iec._61850._2003.scl.TExtRef;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class SVExtRefComparatorByDataFlow implements Comparator<TExtRef>
{
    public SVExtRefComparatorByDataFlow()
    {
    }

    @Override
    public int compare(TExtRef extRef1, TExtRef extRef2)
    {

        Supplier<String> extRef1LnParameter = buildParameter(List.of(extRef1::getPrefix, extRef1::getLnClass, extRef1::getLnInst));
        Supplier<String> extRef2LnParameter = buildParameter(List.of(extRef2::getPrefix, extRef2::getLnClass, extRef2::getLnInst));

        Supplier<String> extRef1DataFlowParameter = buildParameter(List.of(extRef1::getDaName, extRef1::getDoName));
        Supplier<String> extRef2DataFlowParameter = buildParameter(List.of(extRef2::getDaName, extRef2::getDoName));
        int[] equalResults = new int[]{
                equalsParameter(extRef1::getIedName, extRef2::getIedName),
                equalsParameter(extRef1::getLdInst, extRef2::getLdInst),
                equalsParameter(extRef1LnParameter, extRef2LnParameter),
                equalsParameter(extRef1DataFlowParameter, extRef2DataFlowParameter),
                equalsParameter(extRef1::getSrcCBName, extRef2::getSrcCBName)
        };

        for (int equalResult : equalResults)
        {
            if (equalResult != 0)
            {
                return equalResult;
            }
        }

        return 0;
    }

    private Supplier<String> buildParameter(List<Supplier<String>> list)
    {
        StringBuilder outParameter = new StringBuilder();
        int counterOfNulls = 0;
        for (int i = 0; i < list.size(); i++)
        {
            if (list.get(i).get() != null)
            {
                outParameter.append(list.get(i).get());
                continue;
            }
            counterOfNulls++;
        }
        if (counterOfNulls == list.size())
        {
            return () -> null;
        }
        return outParameter::toString;
    }

    private int equalsParameter(Supplier<String> extRef1Param, Supplier<String> extRef2Param)
    {
        if (Objects.isNull(extRef1Param.get()) && Objects.isNull(extRef2Param.get()))
        {
            return 0;
        }

        if (Objects.isNull(extRef1Param.get()) && !Objects.isNull(extRef2Param.get()))
        {
            return -1;
        }

        if (!Objects.isNull(extRef1Param.get()) && Objects.isNull(extRef2Param.get()))
        {
            return 1;
        }

        return extRef1Param.get().compareTo(extRef2Param.get());

    }
}
