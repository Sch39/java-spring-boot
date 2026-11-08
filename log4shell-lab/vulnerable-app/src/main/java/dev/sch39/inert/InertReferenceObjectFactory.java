package dev.sch39.inert;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * ObjectFactory yang hanya menghasilkan objek penanda.
 *
 * Kontrak keamanan:
 * - Tidak menjalankan command, tidak membuka socket, tidak membaca file.
 * - Tidak melakukan class loading dari sumber eksternal.
 * - Hanya membaca atribut yang memang dikirim oleh LDAP server lab.
 *
 * Factory ini ada di classpath lokal supaya JNDI dapat me-resolve
 * Reference Object tanpa perlu remote codebase.
 */
public class InertReferenceObjectFactory implements ObjectFactory {

    private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager
            .getLogger(InertReferenceObjectFactory.class);

    @Override
    public Object getObjectInstance(Object obj,
            Name name,
            Context nameCtx,
            Hashtable<?, ?> environment) {

        // Hanya tangani Reference; abaikan tipe lain.
        if (!(obj instanceof javax.naming.Reference)) {
            return null;
        }

        javax.naming.Reference ref = (javax.naming.Reference) obj;

        // Ambil metadata yang dikirim server. Semua opsional.
        String type = ref.getClassName() != null
                ? ref.getClassName()
                : "UNKNOWN";

        String source = "LOCAL_FACTORY";
        if (ref.get("source") != null) {
            source = String.valueOf(ref.get("source").getContent());
        }

        log.info("[LAB] InertReferenceObjectFactory invoked for type={}", type);

        // Marker: resolusi berhasil, tapi tidak ada eksekusi.
        return new InertReferenceObject(type, source, false);
    }
}