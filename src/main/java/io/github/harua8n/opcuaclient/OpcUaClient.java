package io.github.harua8n.opcuaclient;

public class OpcUaClient {
    public static void main(String[] args)
    {
        String url = args[0];
        String nodeId = args[1];
        OpcUaHelper opc = new OpcUaHelper(url);
        System.out.println(String.format("Data : %s",opc.Read(nodeId)));
        opc.Close();


        System.out.println(String.format("args length : %d",args.length ));
        System.out.println("Hello world");
    }
}
