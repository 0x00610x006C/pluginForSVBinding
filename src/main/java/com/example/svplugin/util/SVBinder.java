package com.example.svplugin.util;

import com.example.svplugin.util.comparators.SVExtRefComparatorByDataFlow;
import com.sclTools.ch.iec._61850._2003.scl.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SVBinder
{
    private final SCL scl;

    private final Set<SVField> svFieldSet;

    public SVBinder(SCL scl)
    {
        this.scl = scl;
        svFieldSet = new HashSet<>();
    }

    public void bind()
    {
        parsePrivateSVFields();

        bindParamsFromCommunication();

        bindDataset();


        for (String iedNameSubscriber : svFieldSet.stream().map(SVField::getIedRecipient).collect(Collectors.toSet()))
        {
            Optional<LN0> ln0 =
                    Stream.of(scl)
                            .flatMap(scl1 -> scl1.getIED().stream())
                            .filter(ied -> ied.getName().equals(iedNameSubscriber))
                            .flatMap(ied -> ied.getAccessPoint().stream())
                            .map(TAccessPoint::getServer)
                            .flatMap(server -> server.getLDevice().stream())
                            .map(TLDevice::getLN0)
                            .findFirst();

            svFieldSet.stream().filter(svField -> iedNameSubscriber.equals(svField.getIedRecipient())).forEach(svField -> ln0.ifPresent(ln01 ->
            {

                System.out.println("Подписчик " + iedNameSubscriber + ", издатель " + svField.getMac());
                if (ln01.getInputs() == null)
                {
                    ln01.setInputs(new TInputs());
                    System.out.println("Не было <Inputs> тэг будет сгенерирован");
                }

                List<TExtRef> extRefList = ln01.getInputs().getExtRef();
                if (extRefList.isEmpty())
                {
                    extRefList.addAll(svField.extRefList());
                    System.out.println("Список входных сигналов пуст. Поиск и замена ренне оформленных подписок на SV не будет произведен");
                    return;
                }

                List<TExtRef> smvExtRef = getAllWhereServiceTypeIsNonNull(extRefList);
                smvExtRef.sort(new SVExtRefComparatorByDataFlow());

                for (int i = 0; i < svField.extRefList().size(); i++)
                {
                    int index = Collections.binarySearch(smvExtRef, svField.extRefList().get(i), new SVExtRefComparatorByDataFlow());
                    if (0 <= index && index <= smvExtRef.size() - 1)
                    {
                        TExtRef extRef = smvExtRef.get(index);
                        System.out.println("Подписка на " + extRef.getIedName() + "." + extRef.getLdInst() + "." + extRef.getPrefix() + extRef.getLnClass() + extRef.getLnInst() + "." + extRef.getDoName() + "." + extRef.getDaName() + " --> " + extRef.getIntAddr() + " будет обновлена");
                        smvExtRef.set(index, svField.extRefList().get(i));
                        continue;
                    }

                    extRefList.add(svField.extRefList().get(i));
                    TExtRef extRef = svField.extRefList().get(i);
                    System.out.println("Добавлена новая подписка " + extRef.getIedName() + "." + extRef.getLdInst() + "." + extRef.getPrefix() + extRef.getLnClass() + extRef.getLnInst() + "." + extRef.getDoName() + "." + extRef.getDaName() + " --> " + extRef.getIntAddr());
                }
            }));
        }
    }

    private List<TExtRef> getAllWhereServiceTypeIsNonNull(List<TExtRef> extRefs)
    {
        List<TExtRef> smvExtRef = new ArrayList<>();
        for (TExtRef extRef : extRefs)
        {
            if (!Objects.isNull(extRef.getServiceType()))
            {
                smvExtRef.add(extRef);
            }
        }
        return smvExtRef;
    }

    private void bindDataset()
    {
        for (SVField svField : svFieldSet)
        {
            Optional<LN0> acceptorLn0 =
                    Stream.of(scl)
                            .flatMap(scl1 -> scl1.getIED().stream())
                            .filter(ied -> ied.getName().equals(svField.getIedAcceptor()))
                            .flatMap(ied -> ied.getAccessPoint().stream())
                            .map(TAccessPoint::getServer)
                            .flatMap(server -> server.getLDevice().stream())
                            .filter(ld -> ld.getInst().equals(svField.getLdInst()))
                            .map(TLDevice::getLN0)
                            .findFirst();

            Optional<TSampledValueControl> sampledValueControl = acceptorLn0.stream()
                    .flatMap(item -> item.getSampledValueControl().stream())
                    .findFirst();

            sampledValueControl.ifPresent(svControl -> {
                svControl.getIEDName().add(GenericBuilder.of(TControlWithIEDName.IEDName::new)
                        .with(TControlWithIEDName.IEDName::setValue, svField.getIedRecipient())
                        .build());
                System.out.println("В список подписчиков " + svField.getIedAcceptor() + "/" + svField.getLdInst() + "/" + svField.getSrcCBName() + "/" + svField.getMac() + " добавлен новый подписчик " + svField.getIedRecipient());
            });


            String acceptorDataset = sampledValueControl
                    .stream()
                    .findFirst()
                    .map(TSampledValueControl::getDatSet)
                    .orElse("__NotConfigured");

            acceptorLn0.stream()
                    .flatMap(item -> item.getDataSet().stream())
                    .filter(dataset -> dataset.getName().equals(acceptorDataset))
                    .flatMap(dataset -> dataset.getFCDA().stream())
                    .forEach(svField::getParamsFromFCDA);
        }
    }

    private void parsePrivateSVFields()
    {
        HashSet<SVField> svFieldList = new HashSet<>();

        int counterOfInputSV = 0;

        for (TIED ied : scl.getIED())
        {
            List<TPrivate> svFields = ied.getPrivate().stream().filter(field -> Arrays.asList(field.getType().split("-")).contains("SVIn")).toList();

            String mac = "";
            String smvID = "";
            for (int i = 0, id = 0; i < svFields.size(); i++)
            {
               String content = svFields.get(i).getContent().isEmpty() ? "" : (String) svFields.get(i).getContent().get(0);
               if (i % 4 == 0)
                {
                    mac = content;
                }

               if (i % 4 == 2 && content.isEmpty())
               {
                   i++;
                   continue;
               }

               if (i % 4 == 2)
               {
                   smvID = content;
               }

                if (i % 4 == 3)
                {
                    if (!svFieldList.add(new SVField(ied.getName(), mac, smvID, id++)))
                    {
                        System.out.println("------ Устройство " + ied.getName() + " найден дублирующийся входящий SV поток с mac-address " + mac + " и smvID " + smvID);
                    }
                    else
                    {
                        System.out.println("Запись №" + counterOfInputSV++ + " Устройство " + ied.getName() + " найден входящий SV поток с mac-address " + mac + " и smvID " + smvID);
                    }
                }
            }
        }

        this.svFieldSet.addAll(svFieldList);
    }

    private void bindParamsFromCommunication()
    {
        List<TConnectedAP> apWithSV =
                Stream.of(scl)
                        .map(SCL::getCommunication)
                        .flatMap(tCommunication -> tCommunication.getSubNetwork().stream())
                        .flatMap(tSubNetwork -> tSubNetwork.getConnectedAP().stream())
                        .filter(tConnectedAP -> !tConnectedAP.getSMV().isEmpty())
                        .toList();

        for (TConnectedAP ap : apWithSV)
        {
            for (TSMV smv : ap.getSMV())
            {
                Optional.ofNullable(smv.getAddress())
                        .flatMap(address -> address.getP().stream()
                                .filter(p -> ("MAC" + "-Address").equals(p.getType()))
                                .findFirst())
                        .ifPresent(mac ->
                        {
                            String macCompleted = "";

                            if (mac.getValue().length() == 12)
                            {
                                macCompleted = mac.getValue();
                            }
                            if (mac.getValue().contains("-"))
                            {
                                macCompleted = mac.getValue().replace("-", "");
                            }

                            String finalMacCompleted = macCompleted;
                            System.out.println("Устройство " + ap.getIedName() + " имеет SampledValueControl " + smv.getLdInst() + "/" + smv.getCbName() + " и публикует SV поток с mac " + finalMacCompleted);

                            int counterOfSubscribers = 0;
                            for (SVField svField : svFieldSet)
                            {
                                if (finalMacCompleted.equals(svField.getMac()))
                                {
                                    svField.setIedAcceptor(ap.getIedName());
                                    svField.setLdInst(smv.getLdInst());
                                    svField.setSrcCBName(smv.getCbName());
                                    svField.setContainsInFile(true);
                                    counterOfSubscribers++;
                                }
                            }
                            System.out.println("Количество подписчиков " + counterOfSubscribers);
                        });
            }
        }

        int counterOfRemoved = 1;

        for (Iterator<SVField> iterator = svFieldSet.iterator(); iterator.hasNext();)
        {
            SVField item = iterator.next();
            if (!item.isContainsInFile())
            {
                System.out.println("Запись №" + counterOfRemoved++ + " SV поток с mac-address " + item.getMac() + " и smvID " + item.getSmvID() + " не найден в проекте");
                iterator.remove();
            }
        }

        System.out.println("\nПодготовка завершена. Начато оформление подписок");
    }
}
