package dev.sch39.attacker;

import javax.naming.Reference;
import javax.naming.StringRefAddr;

/**
 * Membangun Reference Object yang aman untuk lab.
 *
 * Prinsip:
 * - className = nama objek penanda di classpath vulnerable-app.
 * - factoryClassName = nama factory LOKAL di classpath vulnerable-app.
 * - TIDAK ada RefAddr bertipe "javaCodebase".
 * - TIDAK ada URL ke server HTTP eksternal.
 * - Hanya metadata statis yang bisa dibaca factory.
 */
public final class InertReferenceFixture {

    public static final String LOCAL_FACTORY_CLASS = "dev.sch39.inert.InertReferenceObjectFactory";

    private InertReferenceFixture() {
    }

    public static Reference build() {
        // arg1 = className (nama objek penanda)
        // arg2 = factoryClassName (factory lokal)
        // arg3 = factoryLocation (null = lokal, tanpa codebase)
        Reference ref = new Reference(
                "dev.sch39.inert.InertReferenceObject",
                LOCAL_FACTORY_CLASS,
                null);

        ref.add(new StringRefAddr("source", "LAB_LDAP_ATTACKER_SERVER"));
        ref.add(new StringRefAddr("status", "RESOLVED"));
        ref.add(new StringRefAddr("executed", "false"));

        // Sengaja TIDAK menambahkan:
        // ref.add(new StringRefAddr("javaCodebase", "http://..."));
        return ref;
    }
}