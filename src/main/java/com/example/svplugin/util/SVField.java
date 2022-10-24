package com.example.svplugin.util;

import com.sclTools.ch.iec._61850._2003.scl.TExtRef;
import com.sclTools.ch.iec._61850._2003.scl.TFCDA;
import com.sclTools.ch.iec._61850._2003.scl.TServiceType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SVField
{
    private final String iedRecipient;

    private final String mac;

    private final String smvID;

    private String iedAcceptor;

    private String ldInst;

    private String srcCBName;

    private final List<TExtRef> extRefList;

    private final int id;

    private boolean containsInFile;

    public SVField(String ied, String mac, String goID, int id)
    {
        this.iedRecipient = ied;
        this.mac = mac;
        this.smvID = goID;
        this.id = id;
        this.extRefList = new ArrayList<>();
        this.containsInFile = false;
    }

    public String getIedRecipient()
    {
        return iedRecipient;
    }

    public String getMac()
    {
        return mac;
    }

    public String getSmvID()
    {
        return smvID;
    }

    public String getIedAcceptor()
    {
        return iedAcceptor;
    }

    public void setIedAcceptor(String iedAcceptor)
    {
        this.iedAcceptor = iedAcceptor;
    }

    public String getLdInst()
    {
        return ldInst;
    }

    public void setLdInst(String ldInst)
    {
        this.ldInst = ldInst;
    }

    public String getSrcCBName()
    {
        return srcCBName;
    }

    public void setSrcCBName(String srcCBName)
    {
        this.srcCBName = srcCBName;
    }

    public List<TExtRef> extRefList()
    {
        return extRefList;
    }

    public int getId()
    {
        return id;
    }

    public boolean isContainsInFile()
    {
        return containsInFile;
    }

    public void setContainsInFile(boolean containsInFile)
    {
        this.containsInFile = containsInFile;
    }

    public void getParamsFromFCDA(TFCDA fcda)
    {
        String intAddress = configureIntAddr(fcda);

        extRefList.add(
                GenericBuilder.of(TExtRef::new)
                        .with(TExtRef::setSrcCBName, srcCBName)
                        .with(TExtRef::setIedName, iedAcceptor)
                        .with(TExtRef::setLdInst, ldInst)
                        .with(TExtRef::setServiceType, TServiceType.SMV)
                        .with(TExtRef::setPrefix, fcda.getPrefix())
                        .with(TExtRef::setLnClass, fcda.getLnClass())
                        .with(TExtRef::setLnInst, fcda.getLnInst())
                        .with(TExtRef::setDoName, fcda.getDoName())
                        .with(TExtRef::setDaName, fcda.getDaName())
                        .with(TExtRef::setIntAddr, intAddress)
                        .build()
        );
    }

    @NotNull
    private String configureIntAddr(TFCDA fcda)
    {
        int x = id / 16 + 1;
        int y = id % 16;
        String measValue = "TCTR".equals(fcda.getLnClass()) ? "I" : "TVTR".equals(fcda.getLnClass()) ? "U" : "";
        String phase = fcda.getLnInst();
        boolean parity = y % 2 == 1;

        StringBuilder builder = new StringBuilder("SV").append(x).append("-").append(y + 1).append(" ").append(measValue).append(phase);

        if (parity)
        {
            builder.append(" R");
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        SVField field = (SVField) o;
        return iedRecipient.equals(field.iedRecipient) && mac.equals(field.mac);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(iedRecipient, mac);
    }
}
