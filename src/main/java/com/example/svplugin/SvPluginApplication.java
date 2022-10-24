package com.example.svplugin;

import com.example.svplugin.util.Help;
import com.example.svplugin.util.SVBinder;
import com.sclTools.ch.iec._61850._2003.scl.SCL;
import com.sclTools.compositorsSCL.PrettyPrint;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class SvPluginApplication
{
    public static void main(String[] args)
    {
        if (args.length <= 0)
        {
            System.out.println("Не передано ни одного аргумента" + Help.help);
            return;
        }

        if (args.length > 1)
        {
            System.out.println("В качестве аргумента возможно передать -help, либо название файла.");
        }

        if ("-help".equals(args[0]))
        {
            System.out.println(Help.help);
            return;
        }

        SCL scl =  getSCLContent(args);

        bindSV(scl);
    }

    private static SCL getSCLContent(String[] args)
    {
        System.out.println("Подготовка к извлечению данных");
        Document document = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        String fileName = args[0];
        try(FileInputStream fileReader = new FileInputStream(fileName))
        {
            document = factory.newDocumentBuilder().parse(fileReader);
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            e.printStackTrace();
        }

        if (document != null)
        {
            document.normalizeDocument();
            System.out.println("Данные из документа загружены");
            return new SCL(document.getDocumentElement());
        }

        return null;
    }

    private static void bindSV(SCL scl)
    {
        if (scl != null)
        {
            System.out.println("Начата подготовка к офоромлению подписок");
            SVBinder binder = new SVBinder(scl);
            binder.bind();

            try
            {
                System.out.println("Подготовка к формированию документа...");
                JAXBContext jaxbContext = JAXBContext.newInstance(SCL.class);
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

                System.out.println("Старт формирования документа. Документ будет назван outWithSV.xml");
                jaxbMarshaller.marshal(scl, new File("intermediate"));
                PrettyPrint prettyPrint = new PrettyPrint();
                prettyPrint.PrintPretty("intermediate", "outWithSV.xml");
                System.out.println("Документ готов.");
            }
            catch (JAXBException | IOException | TransformerException | SAXException e)
            {
                e.printStackTrace();
            }
        }
    }
}
