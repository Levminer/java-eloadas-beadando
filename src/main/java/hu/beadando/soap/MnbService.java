package hu.beadando.soap;

import hu.beadando.soap.mnb.MNBArfolyamServiceSoap;
import hu.beadando.soap.mnb.MNBArfolyamServiceSoapImpl;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SOAP kliens a Magyar Nemzeti Bank árfolyam webszolgáltatásához
 * (http://www.mnb.hu/arfolyamok.asmx).
 * <p>
 * A kliens osztályokat (hu.beadando.soap.mnb) a jaxws-maven-plugin generálja
 * a src/main/wsdl mappában lévő WSDL-ből. A webszolgáltatás XML szövegként
 * adja vissza az eredményt, ezt itt dolgozzuk fel.
 */
@Service
public class MnbService {

	private MNBArfolyamServiceSoap port;

	/** A SOAP port létrehozása csak az első híváskor, mert ehhez letölti a WSDL-t. */
	private synchronized MNBArfolyamServiceSoap getPort() {
		if (port == null) {
			port = new MNBArfolyamServiceSoapImpl().getCustomBindingMNBArfolyamServiceSoap();
		}
		return port;
	}

	/**
	 * Az MNB által jegyzett devizák listája (a HUF nélkül).
	 * Válasz formátuma: {@code <MNBCurrencies><Currencies><Curr>EUR</Curr>...}
	 */
	public List<String> getCurrencies() {
		try {
			Document doc = parseXml(getPort().getCurrencies());
			NodeList nodes = doc.getElementsByTagName("Curr");
			List<String> currencies = new ArrayList<>();
			for (int i = 0; i < nodes.getLength(); i++) {
				String curr = nodes.item(i).getTextContent().trim();
				if (!curr.equals("HUF")) {
					currencies.add(curr);
				}
			}
			return currencies;
		} catch (Exception e) {
			throw new MnbException("Nem sikerült lekérdezni a devizák listáját az MNB-től.", e);
		}
	}

	/**
	 * Egy deviza árfolyamai a megadott időszakban, dátum szerint növekvő sorrendben.
	 * Válasz formátuma: {@code <MNBExchangeRates><Day date="2026-01-02"><Rate unit="1" curr="EUR">382,65</Rate></Day>...}
	 */
	public List<ExchangeRate> getExchangeRates(String currency, LocalDate startDate, LocalDate endDate) {
		try {
			String xml = getPort().getExchangeRates(startDate.toString(), endDate.toString(), currency);
			Document doc = parseXml(xml);
			NodeList days = doc.getElementsByTagName("Day");
			List<ExchangeRate> rates = new ArrayList<>();
			for (int i = 0; i < days.getLength(); i++) {
				Element day = (Element) days.item(i);
				LocalDate date = LocalDate.parse(day.getAttribute("date"));
				NodeList dayRates = day.getElementsByTagName("Rate");
				for (int j = 0; j < dayRates.getLength(); j++) {
					Element rate = (Element) dayRates.item(j);
					rates.add(new ExchangeRate(
							date,
							rate.getAttribute("curr"),
							Integer.parseInt(rate.getAttribute("unit")),
							new BigDecimal(rate.getTextContent().trim().replace(',', '.'))));
				}
			}
			rates.sort(Comparator.comparing(ExchangeRate::date));
			return rates;
		} catch (Exception e) {
			throw new MnbException("Nem sikerült lekérdezni az árfolyamokat az MNB-től.", e);
		}
	}

	private static Document parseXml(String xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	/** Hiba az MNB webszolgáltatás hívása vagy a válasz feldolgozása közben. */
	public static class MnbException extends RuntimeException {
		public MnbException(String message, Throwable cause) {
			super(message, cause);
		}
	}

}
