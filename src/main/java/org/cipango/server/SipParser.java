package org.cipango.server;

import org.cipango.sip.SipRules;
import org.cipango.sip.SipVersion;
import org.cipango.util.Scanner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

public class SipParser {

    enum State {
        START, HEADER, END;
    }

    public static final byte CR = 0x0D;
    public static final byte LF = 0x0A;

    private ByteBuffer lineBuffer = ByteBuffer.allocate(2048);

    private byte eol;
    private State state = State.START;

    private String header;

    public void parse(ByteBuffer buffer) throws ParseException, IOException {

        while (buffer.hasRemaining()) {
            byte b = buffer.get();

            if (eol == CR && b == LF) {
                eol = LF;
                continue;
            }
            eol = 0;

            if (!lineBuffer.hasRemaining())
                throw new IOException("Line too large");

            if (b == CR || b == LF) {
                eol = b;
                parseLine(takeString());
            } else {
                lineBuffer.put(b);
            }
        }
    }

    protected String takeString() {
        lineBuffer.flip();

        String line = StandardCharsets.UTF_8.decode(lineBuffer).toString();
        lineBuffer.clear();

        return line;
    }

    protected void parseLine(String line) throws ParseException {

        if (state == State.START) {
            if (!isResponse(line)) {

                Scanner scanner = new Scanner(line);
                String method = scanner.token();
                String uri = scanner.matchSpace().readUntilSpace();


                state = State.HEADER;
            }
        } else if (state == State.HEADER) {
            if (header != null)

            if (line.length() == 0)
                state = State.END;
            else
                header = line;
        }

    }

    protected boolean isResponse(String line) {
        return line.startsWith(SipVersion.SIP_2_0.asString());
    }

    class Handler {


        public void requestLine() {

        }
    }

    public static void main(String[] args) throws Exception {
        String s = "INVITE sip:service@192.168.1.26:5070 SIP/2.0\r\n"
        + "Via: SIP/2.0/UDP 192.168.1.26:5060;branch=z9hG4bK-90021-1-0\r\n"
        + "From: sipp <sip:sipp@192.168.1.26:5060>;tag=90021SIPpTag001\r\n"
        + "To: service <sip:service@192.168.1.26:5070>\r\n"
        + "Call-ID: 1-90021@192.168.1.26\r\n"
        + "CSeq: 1 INVITE\r\n"
        + "Contact: sip:sipp@192.168.1.26:5060\r\n"
        + "Max-Forwards: 70\r\n"
        + "Subject: Performance Test\r\n"
        + "Content-Type: application/sdp\r\n"
        + "Content-Length:   135\r\n"
        + "\r\n"
        + "v=0\r\n"
        + "o=user1 53655765 2353687637 IN IP4 192.168.1.26\r\n"
        + "s=-\r\n"
        + "c=IN IP4 192.168.1.26\r\n"
        + "t=0 0\r\n"
        + "m=audio 6000 RTP/AVP 0\r\n"
        + "a=rtpmap:0 PCMU/8000\r\n";

        new SipParser().parse(StandardCharsets.UTF_8.encode(s));
    }
}
