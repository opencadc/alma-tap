
package org.opencadc.tap.integration;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.net.ContentType;
import ca.nrc.cadc.net.HttpDownload;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.tap.schema.SchemaDesc;
import ca.nrc.cadc.tap.schema.TableDesc;
import ca.nrc.cadc.tap.schema.TapSchema;
import ca.nrc.cadc.util.Log4jInit;
import ca.nrc.cadc.vosi.TableReader;
import ca.nrc.cadc.vosi.TableSetReader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;

/**
 * @author pdowler
 */
public class VosiTablesTest {
    private static final Logger log = Logger.getLogger(VosiTablesTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.tap.integration", Level.INFO);
    }

    URL tablesURL;

    public VosiTablesTest() {
        RegistryClient rc = new RegistryClient();
        this.tablesURL = rc.getServiceURL(URI.create("ivo://cadc.nrc.ca/tap"), Standards.VOSI_TABLES_11,
                                          AuthMethod.ANON);
        log.info(String.format("VosiTablesTest: Using URL %s", this.tablesURL));
    }

    @Test
    public void testValidateTablesetDoc() throws Exception {
        try {
            TableSetReader tsr = new TableSetReader(true);
            log.info("testValidateTablesetDoc: " + tablesURL.toExternalForm());

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(tablesURL, bos);
            get.run();
            Assert.assertEquals(200, get.getResponseCode());
            ContentType ct = new ContentType(get.getContentType());
            Assert.assertEquals("text/xml", ct.getBaseType());

            TapSchema ts = tsr.read(new ByteArrayInputStream(bos.toByteArray()));
            Assert.assertNotNull(ts);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            throw unexpected;
        }
    }

    @Test
    public void testValidateTableDoc() throws Exception {
        try {
            TableReader tr = new TableReader(true);
            String s = tablesURL.toExternalForm() + "/ALMA.ASA_TABLES";
            log.info("testValidateTableDoc: " + s);

            URL url = new URL(s);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(url, bos);
            get.run();
            Assert.assertEquals(String.format("Wrong response code from %s", s), 200, get.getResponseCode());
            ContentType ct = new ContentType(get.getContentType());
            Assert.assertEquals("text/xml", ct.getBaseType());

            TableDesc td = tr.read(new ByteArrayInputStream(bos.toByteArray()));
            Assert.assertNotNull(td);
            Assert.assertEquals("TAP_SCHEMA.schemas", td.getTableName());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            throw unexpected;
        }
    }

    @Test
    public void testTableNotFound() throws Exception {
        try {
            String s = tablesURL.toExternalForm() + "/no_such_table";
            log.info("testTableNotFound: " + s);

            URL url = new URL(s);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            HttpDownload get = new HttpDownload(url, bos);
            get.run();
            Assert.assertEquals(404, get.getResponseCode());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            throw unexpected;
        }
    }

    @Test
    public void testDetailMin() throws Exception {
        try {
            TableSetReader tsr = new TableSetReader(true);
            String s = tablesURL.toExternalForm() + "?detail=min";
            log.info("testDetailMin: " + s);

            URL url = new URL(s);
            TapSchema ts = tsr.read(url.openStream());
            Assert.assertNotNull(ts);
            Assert.assertFalse(ts.getSchemaDescs().isEmpty());
            for (SchemaDesc sd : ts.getSchemaDescs()) {
                log.debug("testDetailMin: " + sd.getSchemaName());
                Assert.assertFalse(sd.getTableDescs().isEmpty());
                for (TableDesc td : sd.getTableDescs()) {
                    Assert.assertTrue("no columns:" + td.getTableName(), td.getColumnDescs().isEmpty());
                }
            }
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            throw unexpected;
        }
    }
}
