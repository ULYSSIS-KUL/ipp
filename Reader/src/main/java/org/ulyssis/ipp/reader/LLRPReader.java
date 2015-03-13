package org.ulyssis.ipp.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.llrp.ltk.exceptions.InvalidLLRPMessageException;
import org.llrp.ltk.generated.enumerations.AISpecStopTriggerType;
import org.llrp.ltk.generated.enumerations.AirProtocols;
import org.llrp.ltk.generated.enumerations.GetReaderCapabilitiesRequestedData;
import org.llrp.ltk.generated.enumerations.ROReportTriggerType;
import org.llrp.ltk.generated.enumerations.ROSpecStartTriggerType;
import org.llrp.ltk.generated.enumerations.ROSpecState;
import org.llrp.ltk.generated.enumerations.ROSpecStopTriggerType;
import org.llrp.ltk.generated.enumerations.StatusCode;
import org.llrp.ltk.generated.interfaces.EPCParameter;
import org.llrp.ltk.generated.messages.ADD_ROSPEC;
import org.llrp.ltk.generated.messages.ADD_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.messages.DELETE_ROSPEC;
import org.llrp.ltk.generated.messages.DELETE_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.messages.ENABLE_ROSPEC;
import org.llrp.ltk.generated.messages.ENABLE_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.messages.GET_READER_CAPABILITIES;
import org.llrp.ltk.generated.messages.GET_READER_CAPABILITIES_RESPONSE;
import org.llrp.ltk.generated.messages.START_ROSPEC;
import org.llrp.ltk.generated.messages.START_ROSPEC_RESPONSE;
import org.llrp.ltk.generated.parameters.AISpec;
import org.llrp.ltk.generated.parameters.AISpecStopTrigger;
import org.llrp.ltk.generated.parameters.AntennaConfiguration;
import org.llrp.ltk.generated.parameters.InventoryParameterSpec;
import org.llrp.ltk.generated.parameters.RFTransmitter;
import org.llrp.ltk.generated.parameters.ROBoundarySpec;
import org.llrp.ltk.generated.parameters.ROReportSpec;
import org.llrp.ltk.generated.parameters.ROSpec;
import org.llrp.ltk.generated.parameters.ROSpecStartTrigger;
import org.llrp.ltk.generated.parameters.ROSpecStopTrigger;
import org.llrp.ltk.generated.parameters.TagReportContentSelector;
import org.llrp.ltk.generated.parameters.TransmitPowerLevelTableEntry;
import org.llrp.ltk.net.LLRPConnectionAttemptFailedException;
import org.llrp.ltk.net.LLRPConnector;
import org.llrp.ltk.net.LLRPEndpoint;
import org.llrp.ltk.types.Bit;
import org.llrp.ltk.types.LLRPMessage;
import org.llrp.ltk.types.UnsignedByte;
import org.llrp.ltk.types.UnsignedInteger;
import org.llrp.ltk.types.UnsignedShort;
import org.llrp.ltk.types.UnsignedShortArray;
import org.ulyssis.ipp.TagId;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * This class is the result of information found on
 * http://learn.impinj.com/articles/en_US/RFID/Reading-and-Writing-User-Memory-with-the-Java-LTK/
 */
public final class LLRPReader implements LLRPEndpoint {
    private static final Logger LOG = LogManager.getLogger(LLRPReader.class);
    
    private LLRPConnector reader;
    private static final int TIMEOUT_MS = 10000;
    private static final long CONNECT_TIMEOUT = 10000L;
    private static final int ROSPEC_ID = 123;
    private final Consumer<LLRPMessage> messageConsumer;
    private final Consumer<String> errorConsumer;

    /**
     * Create a new reader, relay LLRP messages to
     * the given messageConsumer, and errors to the
     * given errorConsumer.
     */
    public LLRPReader(Consumer<LLRPMessage> messageConsumer,
                      Consumer<String> errorConsumer) {
        this.messageConsumer = messageConsumer;
        this.errorConsumer = errorConsumer;
    }

    // Build the ROSpec.
    // An ROSpec specifies start and stop triggers,
    // tag report fields, antennas, etc.
    public ROSpec buildROSpec() {
        // Create a Reader Operation Spec (ROSpec).
        ROSpec roSpec = new ROSpec();
        
        roSpec.setPriority(new UnsignedByte(0));
        roSpec.setCurrentState(new ROSpecState(ROSpecState.Disabled));
        roSpec.setROSpecID(new UnsignedInteger(ROSPEC_ID));

        // Set up the ROBoundarySpec
        // This defines the start and stop triggers.
        ROBoundarySpec roBoundarySpec = new ROBoundarySpec();

        // Set the start trigger to null.
        // This means the ROSpec will start as soon as it is enabled.
        ROSpecStartTrigger startTrig = new ROSpecStartTrigger();
        startTrig
                .setROSpecStartTriggerType(new ROSpecStartTriggerType(ROSpecStartTriggerType.Null));
        roBoundarySpec.setROSpecStartTrigger(startTrig);

        // Set the stop trigger is null. This means the ROSpec
        // will keep running until an STOP_ROSPEC message is sent.
        ROSpecStopTrigger stopTrig = new ROSpecStopTrigger();
        stopTrig.setDurationTriggerValue(new UnsignedInteger(0));
        stopTrig.setROSpecStopTriggerType(new ROSpecStopTriggerType(ROSpecStopTriggerType.Null));
        roBoundarySpec.setROSpecStopTrigger(stopTrig);

        roSpec.setROBoundarySpec(roBoundarySpec);

        // Add an Antenna Inventory Spec (AISpec).
        AISpec aispec = new AISpec();

        // Set the AI stop trigger to null. This means that
        // the AI spec will run until the ROSpec stops.
        AISpecStopTrigger aiStopTrigger = new AISpecStopTrigger();
        aiStopTrigger
                .setAISpecStopTriggerType(new AISpecStopTriggerType(AISpecStopTriggerType.Null));
        aiStopTrigger.setDurationTrigger(new UnsignedInteger(0));
        aispec.setAISpecStopTrigger(aiStopTrigger);

        // Select which antenna ports we want to use.
        // Setting this property to zero means all antenna ports.
        UnsignedShortArray antennaIDs = new UnsignedShortArray();
        antennaIDs.add(new UnsignedShort(0));
        aispec.setAntennaIDs(antennaIDs);

        // Tell the reader that we're reading Gen2 tags.
        InventoryParameterSpec inventoryParam = new InventoryParameterSpec();
        inventoryParam.setProtocolID(new AirProtocols(AirProtocols.EPCGlobalClass1Gen2));
        inventoryParam.setInventoryParameterSpecID(new UnsignedShort(1));
        

        roSpec.addToSpecParameterList(aispec);
        
        AntennaConfiguration antConfig = new AntennaConfiguration();
        antConfig.setAntennaID(new UnsignedShort(0));
        
        RFTransmitter tx = new RFTransmitter();
        tx.setTransmitPower(new UnsignedShort(87)); // TODO: Is this the max?
        tx.setChannelIndex(new UnsignedShort(1));
        tx.setHopTableID(new UnsignedShort(1));
        antConfig.setRFTransmitter(tx);
        
        inventoryParam.addToAntennaConfigurationList(antConfig);
        aispec.addToInventoryParameterSpecList(inventoryParam);

        // Specify what type of tag reports we want
        // to receive and when we want to receive them.
        ROReportSpec roReportSpec = new ROReportSpec();
        // Receive a report every time a tag is read.
        roReportSpec.setROReportTrigger(new ROReportTriggerType(
                ROReportTriggerType.Upon_N_Tags_Or_End_Of_ROSpec));
        roReportSpec.setN(new UnsignedShort(1));
        TagReportContentSelector reportContent = new TagReportContentSelector();
        // Select which fields we want in the report.
        reportContent.setEnableAccessSpecID(new Bit(0));
        reportContent.setEnableAntennaID(new Bit(0));
        reportContent.setEnableChannelIndex(new Bit(0));
        reportContent.setEnableFirstSeenTimestamp(new Bit(0));
        reportContent.setEnableInventoryParameterSpecID(new Bit(0));
        reportContent.setEnableLastSeenTimestamp(new Bit(1));
        reportContent.setEnablePeakRSSI(new Bit(0));
        reportContent.setEnableROSpecID(new Bit(0));
        reportContent.setEnableSpecIndex(new Bit(0));
        reportContent.setEnableTagSeenCount(new Bit(0));
        roReportSpec.setTagReportContentSelector(reportContent);
        roSpec.setROReportSpec(roReportSpec);

        return roSpec;
    }

    public List<TransmitPowerLevelTableEntry> readTransmitPowerEntries() throws LLRPException {
        GET_READER_CAPABILITIES_RESPONSE response;

        try {
            GET_READER_CAPABILITIES getReaderCaps = new GET_READER_CAPABILITIES();
            getReaderCaps.setRequestedData(new GetReaderCapabilitiesRequestedData(GetReaderCapabilitiesRequestedData.All));
            response = (GET_READER_CAPABILITIES_RESPONSE) reader.transact(getReaderCaps, TIMEOUT_MS);
            
            StatusCode status = response.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode(StatusCode.M_Success))) {
                return response.getRegulatoryCapabilities().getUHFBandCapabilities().getTransmitPowerLevelTableEntryList();
            } else {
                LOG.error("Error reading capabilities. Status code: {}", status.toString());
                throw new LLRPException();
            }
        } catch (TimeoutException e) {
            LOG.error("Timeout when adding reading capabilities.", e);
            throw new LLRPException(e);
        }
    }

    // Add the ROSpec to the reader.
    public void addROSpec() throws LLRPException {
        ADD_ROSPEC_RESPONSE response;

        ROSpec roSpec = buildROSpec();
        LOG.info("Adding the ROSpec");
        try {
            ADD_ROSPEC roSpecMsg = new ADD_ROSPEC();
            roSpecMsg.setROSpec(roSpec);
            response = (ADD_ROSPEC_RESPONSE) reader.transact(roSpecMsg, TIMEOUT_MS);
            LOG.info("Adding ROSpec response: {}", response.toXMLString());

            // Check if the we successfully added the ROSpec.
            StatusCode status = response.getLLRPStatus().getStatusCode();
            if (status.equals(new StatusCode(StatusCode.M_Success))) {
                 LOG.info("Successfully added ROSpec.");
            } else {
                // TODO: use the status code, Luke!
                LOG.error("Error adding ROSpec. Status code: {}", status.toString());
                throw new LLRPException();
            }
        } catch (TimeoutException e) {
            LOG.error("Timeout when adding ROSpec.", e);
            throw new LLRPException(e);
        } catch (InvalidLLRPMessageException e) {
            LOG.fatal("Formed invalid ADD_ROSPEC message.", e);
            throw new LLRPException(e);
        }
    }

    // Enable the ROSpec.
    public void enableROSpec() throws LLRPException {
        ENABLE_ROSPEC_RESPONSE response;

        LOG.info("Enabling the ROSpec.");
        ENABLE_ROSPEC enable = new ENABLE_ROSPEC();
        enable.setROSpecID(new UnsignedInteger(ROSPEC_ID));
        try {
            response = (ENABLE_ROSPEC_RESPONSE) reader.transact(enable, TIMEOUT_MS);
            LOG.info("ROSpec enable response: {}", response.toXMLString());
        } catch (TimeoutException e) {
            LOG.error("Timeout when enabling ROSpec.", e);
            throw new LLRPException(e);
        } catch (InvalidLLRPMessageException e) {
            LOG.fatal("Formed invalid ENABLE_ROSPEC message.", e);
            throw new LLRPException(e);
        }
    }

    // Start the ROSpec.
    public void startROSpec() throws LLRPException {
        START_ROSPEC_RESPONSE response;
        LOG.info("Starting the ROSpec.");
        START_ROSPEC start = new START_ROSPEC();
        start.setROSpecID(new UnsignedInteger(ROSPEC_ID));
        try {
            response = (START_ROSPEC_RESPONSE) reader.transact(start, TIMEOUT_MS);
            LOG.info("Start ROSpec response: {}", response.toXMLString());
        } catch (TimeoutException e) {
            LOG.error("Timeout when starting ROSpec.", e);
            throw new LLRPException(e);
        } catch (InvalidLLRPMessageException e) {
            LOG.fatal("Formed invalid START_ROSPEC message.", e);
            throw new LLRPException(e);
        }
    }

    // Delete all ROSpecs from the reader.
    public void deleteROSpecs() throws LLRPException {
        DELETE_ROSPEC_RESPONSE response;

        LOG.info("Deleting all ROSpecs.");
        DELETE_ROSPEC del = new DELETE_ROSPEC();
        // Use zero as the ROSpec ID.
        // This means delete all ROSpecs.
        del.setROSpecID(new UnsignedInteger(0));
        try {
            response = (DELETE_ROSPEC_RESPONSE) reader.transact(del, TIMEOUT_MS);
            LOG.info("Delete ROSpec response: {}", response.toXMLString());
        } catch (TimeoutException e) {
            LOG.error("Timeout when deleting ROSpec.", e);
            throw new LLRPException(e);
        } catch (InvalidLLRPMessageException e) {
            LOG.fatal("Formed invalid DELETE_ROSPEC message.", e);
            throw new LLRPException(e);
        }
    }

    // Connect to the reader
    public void connect(URI uri) throws LLRPException {
        // Create the reader object.
        if (uri.getPort() == -1) {
            reader = new LLRPConnector(this, uri.getHost());
        } else {
            reader = new LLRPConnector(this, uri.getHost(), uri.getPort());
        }

        // Try connecting to the reader.
        try {
            LOG.info("Connecting to the reader.");
            // NOTE: The timeout is a lot longer
            reader.connect(CONNECT_TIMEOUT);
        } catch (LLRPConnectionAttemptFailedException e1) {
            LOG.error("Error connecting to the reader", e1);
            throw new LLRPException(e1);
        }
    }

    // Disconnect from the reader
    public void disconnect() {
        reader.disconnect();
    }

    // Connect to the reader, setup the ROSpec
    // and run it.
    public boolean run(URI uri) {
        try {
            connect(uri);
            deleteROSpecs();
            addROSpec();
            enableROSpec();
            startROSpec();
            return true;
        } catch (LLRPException e) {
            return false;
        }
    }

    // Cleanup. Delete all ROSpecs
    // and disconnect from the reader.
    public boolean stop() {
        try {
            deleteROSpecs();
            disconnect();
            return true;
        } catch (LLRPException e) {
            return false;
        }
    }

    @Override
    public void messageReceived(LLRPMessage message) {
        messageConsumer.accept(message);
    }

    @Override
    public void errorOccured(String message) {
        errorConsumer.accept(message);
    }

    public static TagId decodeEPCParameter(EPCParameter epc) {
        // TODO: Use LLRPBitList sublist stuff?
        byte[] epcBytes = epc.encodeBinary().toByteArray();
        if (epcBytes[0] == ((byte)0x8d)) {
            return new TagId(decodeEPC96(epcBytes));
        } else if (epcBytes[1] == ((byte)0xf1)) {
            return new TagId(decodeEPCData(epcBytes));
        } else {
            LOG.error("Couldn't decode EPCParameter {}: unknown format.", new TagId(epcBytes));
            return new TagId(epcBytes);
        }
    }

    private static byte[] decodeEPC96(byte[] epc96) {
        byte[] epc = new byte[12]; // 96 bits is 12 bytes
        System.arraycopy(epc96, 1, epc, 0, 12);
        return epc;
    }

    private static byte[] decodeEPCData(byte[] epcData) {
        int length = 0;
        length += Byte.toUnsignedInt(epcData[4]) << 8;
        length += Byte.toUnsignedInt(epcData[5]);
        // TODO: What if EPC length is not a multiple of 8? Does this occur?
        byte[] epc = new byte[length / 8];
        System.arraycopy(epcData, 6, epc, 0, length / 8);
        return epc;
    }
}
