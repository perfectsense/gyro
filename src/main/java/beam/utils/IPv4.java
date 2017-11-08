/*
Copyright (c) 2010, Saddam Abu Ghaida, Nicolai Tufar
              2013, Nico Coetzee <NCoetzee1@fnb.co.za>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
 * Neither the name of the Saddam Abu Ghaida or Nicolai Tufar nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Saddam Abu Ghaida or Nicolai Tufar BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package beam.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IPv4 {
	int baseIPnumeric;
	int netmaskNumeric;

	/**
	 * Specify IP address and netmask like: new
	 * IPv4("10.1.0.25","255.255.255.16")
	 *
	 * @param symbolicIP
	 * @param netmask
	 */
	public IPv4(String symbolicIP, String netmask) throws NumberFormatException {
		/* IP */
		String[] st = symbolicIP.split("\\.");
		if (st.length != 4)
			throw new NumberFormatException("Invalid IP address: " + symbolicIP);

		int i = 24;
		baseIPnumeric = 0;
		for (int n = 0; n < st.length; n++) {
			int value = Integer.parseInt(st[n]);
			if (value != (value & 0xff)) {
				throw new NumberFormatException("Invalid IP address: "
						+ symbolicIP);
			}

			baseIPnumeric += value << i;
			i -= 8;
		}

		/* Netmask */
		st = netmask.split("\\.");
		if (st.length != 4)
			throw new NumberFormatException("Invalid netmask address: "
					+ netmask);

		i = 24;
		netmaskNumeric = 0;
		if (Integer.parseInt(st[0]) < 255) {
			throw new NumberFormatException(
					"The first byte of netmask can not be less than 255");
		}
		for (int n = 0; n < st.length; n++) {
			int value = Integer.parseInt(st[n]);
			if (value != (value & 0xff)) {
				throw new NumberFormatException("Invalid netmask address: "
						+ netmask);
			}

			netmaskNumeric += value << i;
			i -= 8;
		}

		/*
		 * see if there are zeroes inside netmask, like: 1111111101111 This is
		 * illegal, throw exception if encountered. Netmask should always have
		 * only ones, then only zeroes, like: 11111111110000
		 */
		boolean encounteredOne = false;
		int ourMaskBitPattern = 1;
		for (i = 0; i < 32; i++) {
			if ((netmaskNumeric & ourMaskBitPattern) != 0) {
				encounteredOne = true; // the bit is 1
			} else { // the bit is 0
				if (encounteredOne == true)
					throw new NumberFormatException("Invalid netmask: "
							+ netmask + " (bit " + (i + 1) + ")");
			}

			ourMaskBitPattern = ourMaskBitPattern << 1;
		}

	}

	/**
	 * Specify IP in CIDR format like: new IPv4("10.1.0.25/16");
	 *
	 * @param IPinCIDRFormat
	 */
	public IPv4(String IPinCIDRFormat) throws NumberFormatException {
		String[] st = IPinCIDRFormat.split("\\/");
		if (st.length != 2)
			throw new NumberFormatException("Invalid CIDR format '"
					+ IPinCIDRFormat + "', should be: xx.xx.xx.xx/xx");
		String symbolicIP = st[0];
		String symbolicCIDR = st[1];
		Integer numericCIDR = new Integer(symbolicCIDR);
		if (numericCIDR > 32)
			throw new NumberFormatException("CIDR can not be greater than 32");

		/* IP */
		st = symbolicIP.split("\\.");
		if (st.length != 4)
			throw new NumberFormatException("Invalid IP address: " + symbolicIP);

		int i = 24;
		baseIPnumeric = 0;
		for (int n = 0; n < st.length; n++) {
			int value = Integer.parseInt(st[n]);
			if (value != (value & 0xff)) {
				throw new NumberFormatException("Invalid IP address: "
						+ symbolicIP);
			}

			baseIPnumeric += value << i;
			i -= 8;
		}

		/* netmask from CIDR */
		if (numericCIDR < 8)
			throw new NumberFormatException(
					"Netmask CIDR can not be less than 8");
		netmaskNumeric = 0xffffffff;
		netmaskNumeric = netmaskNumeric << (32 - numericCIDR);
	}

    public boolean isPrivateNetwork() {
        IPv4 private10 = new IPv4("10.0.0.0/8");
        IPv4 private172 = new IPv4("172.16.0.0/12");
        IPv4 private192 = new IPv4("192.168.0.0/16");

        if (private10.contains(this) ||
                private172.contains(this) ||
                private192.contains(this)) {
            return true;
        }

        return false;
    }

	/**
	 * Get the IP in symbolic form, i.e. xxx.xxx.xxx.xxx
	 *
	 * @return
	 */
	public String getIP() {
		return convertNumericIpToSymbolic(baseIPnumeric);
	}

    public String getNthIP(int n) {
        Integer baseIP = baseIPnumeric & netmaskNumeric;
        return convertNumericIpToSymbolic(baseIP + n);
    }

	private String convertNumericIpToSymbolic(Integer ip) {
		StringBuffer sb = new StringBuffer(15);
		for (int shift = 24; shift > 0; shift -= 8) {
			// process 3 bytes, from high order byte down.
			sb.append(Integer.toString((ip >>> shift) & 0xff));
			sb.append('.');
		}
		sb.append(Integer.toString(ip & 0xff));
		return sb.toString();
	}

	/**
	 * Get the net mask in symbolic form, i.e. xxx.xxx.xxx.xxx
	 *
	 * @return
	 */
	public String getNetmask() {
		StringBuffer sb = new StringBuffer(15);
		for (int shift = 24; shift > 0; shift -= 8) {
			// process 3 bytes, from high order byte down.
			sb.append(Integer.toString((netmaskNumeric >>> shift) & 0xff));
			sb.append('.');
		}
		sb.append(Integer.toString(netmaskNumeric & 0xff));
		return sb.toString();
	}

	/**
	 * Get the IP and netmask in CIDR form, i.e. xxx.xxx.xxx.xxx/xx
	 *
	 * @return
	 */

	public String getCIDR() {
		int i;
		for (i = 0; i < 32; i++) {
			if ((netmaskNumeric << i) == 0)
				break;
		}
		return convertNumericIpToSymbolic(baseIPnumeric & netmaskNumeric) + "/"
				+ i;
	}

	/**
	 * Get an arry of all the IP addresses available for the IP and netmask/CIDR
	 * given at initialization
	 *
	 * @return
	 */
	public List<String> getAvailableIPs(Integer numberofIPs) {
		ArrayList<String> result = new ArrayList<String>();
		int numberOfBits;
		for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
			if ((netmaskNumeric << numberOfBits) == 0)
				break;
		}
		Integer numberOfIPs = 0;
		for (int n = 0; n < (32 - numberOfBits); n++) {
			numberOfIPs = numberOfIPs << 1;
			numberOfIPs = numberOfIPs | 0x01;
		}

		Integer baseIP = baseIPnumeric & netmaskNumeric;

		for (int i = 1; i < (numberOfIPs) && i < numberofIPs; i++) {
			Integer ourIP = baseIP + i;

			String ip = convertNumericIpToSymbolic(ourIP);
			result.add(ip);
		}

		return result;

	}

	/**
	 * Range of hosts
	 *
	 * @return
	 */
	public String getHostAddressRange() {
		int numberOfBits;
		for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
			if ((netmaskNumeric << numberOfBits) == 0)
				break;
		}
		Integer numberOfIPs = 0;
		for (int n = 0; n < (32 - numberOfBits); n++) {
			numberOfIPs = numberOfIPs << 1;
			numberOfIPs = numberOfIPs | 0x01;
		}

		Integer baseIP = baseIPnumeric & netmaskNumeric;
		String firstIP = convertNumericIpToSymbolic(baseIP + 1);
		String lastIP = convertNumericIpToSymbolic(baseIP + numberOfIPs - 1);

		return firstIP + " - " + lastIP;
	}

	/**
	 * Returns number of hosts available in given range
	 *
	 * @return number of hosts
	 */
	public Long getNumberOfHosts() {
		int numberOfBits;
		for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
			if ((netmaskNumeric << numberOfBits) == 0)
				break;
		}

		Double x = Math.pow(2, (32 - numberOfBits));
		if (x == -1)
			x = 1D;
		return x.longValue();
	}

	/**
	 * The XOR of the netmask
	 *
	 * @return wildcard mask in text form, i.e. 0.0.15.255
	 */
	public String getWildcardMask() {
		Integer wildcardMask = netmaskNumeric ^ 0xffffffff;
		StringBuffer sb = new StringBuffer(15);
		for (int shift = 24; shift > 0; shift -= 8) {
			// process 3 bytes, from high order byte down.
			sb.append(Integer.toString((wildcardMask >>> shift) & 0xff));
			sb.append('.');
		}
		sb.append(Integer.toString(wildcardMask & 0xff));
		return sb.toString();

	}

	public String getBroadcastAddress() {
		if (netmaskNumeric == 0xffffffff)
			return "0.0.0.0";

		int numberOfBits;
		for (numberOfBits = 0; numberOfBits < 32; numberOfBits++) {
			if ((netmaskNumeric << numberOfBits) == 0)
				break;
		}
		Integer numberOfIPs = 0;
		for (int n = 0; n < (32 - numberOfBits); n++) {
			numberOfIPs = numberOfIPs << 1;
			numberOfIPs = numberOfIPs | 0x01;
		}

		Integer baseIP = baseIPnumeric & netmaskNumeric;
		Integer ourIP = baseIP + numberOfIPs;
		String ip = convertNumericIpToSymbolic(ourIP);

		return ip;
	}

	private String getBinary(Integer number) {
		String result = "";
		Integer ourMaskBitPattern = 1;
		for (int i = 1; i <= 32; i++) {
			if ((number & ourMaskBitPattern) != 0) {
				result = "1" + result; // the bit is 1
			} else { // the bit is 0
				result = "0" + result;
			}
			if ((i % 8) == 0 && i != 0 && i != 32)
				result = "." + result;
			ourMaskBitPattern = ourMaskBitPattern << 1;
		}
		return result;
	}

	public String getNetmaskInBinary() {
		return getBinary(netmaskNumeric);
	}

	/**
	 * Checks if the given IP address contains in subnet
	 *
	 * @param IPaddress
	 * @return
	 */
	public boolean contains(String IPaddress) {
		Integer checkingIP = 0;
		String[] st = IPaddress.split("\\.");
		if (st.length != 4)
			throw new NumberFormatException("Invalid IP address: " + IPaddress);

		int i = 24;
		for (int n = 0; n < st.length; n++) {
			int value = Integer.parseInt(st[n]);
			if (value != (value & 0xff)) {
				throw new NumberFormatException("Invalid IP address: "
						+ IPaddress);
			}

			checkingIP += value << i;
			i -= 8;
		}

		if ((baseIPnumeric & netmaskNumeric) == (checkingIP & netmaskNumeric))
			return true;
		else
			return false;
	}

	public boolean contains(IPv4 child) {

		Integer subnetID = child.baseIPnumeric;
		Integer subnetMask = child.netmaskNumeric;

		if ((subnetID & this.netmaskNumeric) == (this.baseIPnumeric & this.netmaskNumeric)) {
			if ((this.netmaskNumeric < subnetMask) == true
					&& this.baseIPnumeric <= subnetID) {
				return true;
			}

		}
		return false;
	}

	public boolean validateIPAddress() {
		String IPAddress = getIP();
		if (IPAddress.startsWith("0")) {
			return false;
		}

		if (IPAddress.isEmpty()) {
			return false;
		}

		if (IPAddress
				.matches("\\A(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}\\z")) {
			return true;
		}
		return false;
	}

	/**
	 * @author Nico Coetzee <NCoetzee1@fnb.co.za>
	 * @param lowBoundary
	 * @return
	 */
    private String getBoundaryAddr(boolean lowBoundary){
        String result = "";
        String range = getHostAddressRange();
        Pattern rangeRegex = Pattern.compile("(\\d+\\.\\d+\\.\\d+\\.\\d+)\\s+\\-\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+)");
        Matcher m = rangeRegex.matcher(range);
        if( m.matches() ){
            if( lowBoundary ){
                result = m.group(1);
            } else {
                result = m.group(2);
            }
        }
        return result;
    }

	/**
	 * @author Nico Coetzee <NCoetzee1@fnb.co.za>
	 * @param lowBoundary
	 * @return the first IP address in the IP range
	 */
    public String getFirstIPAddr(){
        return getBoundaryAddr(true);
    }

	/**
	 * @author Nico Coetzee <NCoetzee1@fnb.co.za>
	 * @param lowBoundary
	 * @return the last IP address in the IP range
	 */
    public String getLastIPAddr(){
        return getBoundaryAddr(false);
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// ipv4.setIP("10.20.30.5", "255.255.255.200");
		// System.out.println(ipv4.getIP());
		// System.out.println(ipv4.getNetmask());
		// System.out.println(ipv4.getCIDR());

		/*
		 * IPv4 ipv4 = new IPv4("10.1.17.0/20");
		 * System.out.println(ipv4.getIP());
		 * System.out.println(ipv4.getNetmask());
		 * System.out.println(ipv4.getCIDR());
		 *
		 * System.out.println("============= Available IPs ===============");
		 * List<String> availableIPs = ipv4.getAvailableIPs(); int counter=0;
		 * for (String ip : availableIPs) { System.out.print(ip);
		 * System.out.print(" "); counter++; if((counter%10)==0)
		 * System.out.print("\n"); }
		 */

		IPv4 ipv4 = new IPv4("12.12.12.0/16");
		IPv4 ipv4Child = new IPv4("12.12.12.0/17");
		// IPv4 ipv4 = new IPv4("192.168.20.0/16");
		// System.out.println(ipv4.getIP());
		// System.out.println(ipv4.getNetmask());
		// System.out.println(ipv4.getCIDR());
		// System.out.println("======= MATCHES =======");
		// System.out.println(ipv4.getBinary(ipv4.baseIPnumeric));
		// System.out.println(ipv4.getBinary(ipv4.netmaskNumeric));

		System.out.println(ipv4.contains(ipv4Child));

		System.out.println(ipv4.getBinary(ipv4.baseIPnumeric));
		System.out.println(ipv4.getBinary(ipv4.netmaskNumeric));

		System.out.println(ipv4Child.getBinary(ipv4Child.baseIPnumeric));
		System.out.println(ipv4Child.getBinary(ipv4Child.netmaskNumeric));
		System.out.println("==============output================");
		System.out.println(ipv4.contains(ipv4Child));
		// ipv4.contains("192.168.50.11");
		// System.out.println("======= DOES NOT MATCH =======");
		// ipv4.contains("10.2.3.4");
		// System.out.println(ipv4.validateIPAddress());
		// System.out.println(ipv4.getBinary(ipv4.baseIPnumeric));
		// System.out.println(ipv4.getBinary(ipv4.netmaskNumeric));
	}

}
