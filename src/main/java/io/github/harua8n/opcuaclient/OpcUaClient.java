package io.github.harua8n.opcuaclient;

import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;

public class OpcUaClient {
    public static void main(String[] args)
    {
        String url = args[0];
        String path = args[1];
        OpcUaHelper opc = new OpcUaHelper(url);
        Variant v = opc.Read(path);

        System.out.println(String.format("Data : %s",v.toString()));
        UnsignedInteger ui = new UnsignedInteger(v.intValue() + 1);
        opc.Write(path, new Variant(ui));
        opc.Close();
        System.out.println("Program complete... exiting.");
    }
}
