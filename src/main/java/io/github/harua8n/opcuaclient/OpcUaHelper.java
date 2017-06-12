package io.github.harua8n.opcuaclient;

import org.opcfoundation.ua.application.Application;
import org.opcfoundation.ua.application.Client;
import org.opcfoundation.ua.application.SessionChannel;
import org.opcfoundation.ua.builtintypes.*;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.*;
import org.opcfoundation.ua.encoding.EncoderContext;

import java.text.SimpleDateFormat;

public class OpcUaHelper {
    private String url;
    private SessionChannel uaSession = null;
    private Client uaClient = null;
    private Application uaClientApp = null;

    public OpcUaHelper(String url) {
        this.url = url;
    }

    private boolean createUaApp() {
        boolean ret_val = true;
        if (null == uaSession) {
            uaClientApp = new Application();
            uaClient = new Client(uaClientApp);
            try {
                EndpointDescription[] endpoints = uaClient.discoverEndpoints(this.url);
//                System.out.println(endpoints[0].toString());
                uaSession = uaClient.createSessionChannel(endpoints[0]);
                uaSession.activate();
                ret_val = true;
            } catch (ServiceResultException e) {
                e.printStackTrace();
                ret_val = false;
            }
        }
        return ret_val;
    }

    public Variant Read(String path){
        Variant ret=null;
        if(!createUaApp()) {return ret;}

        try {
            ReadResponse readResponse = uaSession.Read(
                    null,
                    500.0,
                    TimestampsToReturn.Source,
                    new ReadValueId(NodeId.parseNodeId(path), Attributes.Value, null, null)
            );
            ret = readResponse.getResults()[0].getValue();
        } catch (ServiceResultException e) {
            e.printStackTrace();
            if(0x800D0000L == e.getStatusCode().getValue().getValue()) //Bad_ServerNotConnected (code=0x800D0000, description="The operation could not complete because the client is not connected to the server.")
            {
                uaSession = null; //clean up connection
                System.out.println("Trying to recover from 0x800D0000L");
            }
        }

        return ret;
    }

    public float[] ReadHda(String path){
        float[] ret = new float[0];
        if(!createUaApp()) {return ret;}

        try {
            ReadRawModifiedDetails readDetails = new ReadRawModifiedDetails();
            readDetails.setStartTime(DateTime.fromMillis(System.currentTimeMillis() - 2*60*1000));
            readDetails.setEndTime(DateTime.fromMillis(System.currentTimeMillis()));
            readDetails.setIsReadModified(false);
            readDetails.setNumValuesPerNode(UnsignedInteger.valueOf(200));
            readDetails.setReturnBounds(false);

            EncoderContext encoderContext = uaClient.getEncoderContext();
            ExtensionObject eo;
            eo = ExtensionObject.binaryEncode(readDetails, encoderContext);

            HistoryReadValueId hrv = new HistoryReadValueId();
//            hrv.setNodeId(NodeId.parseNodeId("ns=4;s=2:TESTMOD/SGGN1/OUT.CV"));
            hrv.setNodeId(NodeId.parseNodeId(path));


            HistoryReadResponse readResponse = uaSession.HistoryRead(null, eo, TimestampsToReturn.Both, false, hrv);
            HistoryReadResult[] hr = readResponse.getResults();
            System.out.println(hr.length);
            HistoryData historyData = null;
            ExtensionObject eo_data = hr[0].getHistoryData();
            historyData = eo_data.decode(encoderContext);
            System.out.println(historyData.getDataValues().length);
            DataValue[] data = historyData.getDataValues();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
            DateTime dt;
            int max_data = data.length;
            ret = new float[max_data];
            for (int i = 0; i < data.length; i++) {
                ret[i] = data[i].getValue().floatValue();
                dt = data[i].getSourceTimestamp();
                dt.setUseLocalTimeInToString(true);
                System.out.println(String.format("%s:%.04f",dt.toString(),data[i].getValue().floatValue()));
            }
        } catch (ServiceResultException e) {
            e.printStackTrace();
            if(0x800D0000L == e.getStatusCode().getValue().getValue()) //Bad_ServerNotConnected (code=0x800D0000, description="The operation could not complete because the client is not connected to the server.")
            {
                uaSession = null; //clean up connection
                System.out.println("Trying to recover from 0x800D0000L");
            }
        }

        return ret;
    }

    public boolean Write(String path, Variant val)
    {
        NodeId nodeId= NodeId.parseNodeId(path);
        WriteValue writeValue[] = new WriteValue[1];
        DataValue dataValue = new DataValue(val);

        writeValue[0] = new WriteValue(nodeId, Attributes.Value, "", dataValue);
        WriteRequest writeRequest = new WriteRequest(null, writeValue);

        try {
            WriteResponse response = uaSession.Write(writeRequest);
//            System.out.println(response);
        } catch (ServiceResultException e) {
            e.printStackTrace();
        }

        return false;
    }

    public void Close() {
        try {
            System.out.println("Closing...");
            uaSession.close();
            System.out.println("Closing...done");
        } catch (ServiceResultException e) {
            e.printStackTrace();
        }
        uaSession.closeAsync();
    }
}
