package dev.sch39.attacker;

import com.unboundid.ldap.sdk.Entry;

/**
 * Fixture A: entry directory biasa.
 *
 * Tidak mengandung atribut javaNamingReference, javaClassName,
 * javaFactory, atau javaCodebase. Entry ini hanya membuktikan
 * bahwa aplikasi berhasil menghubungi LDAP server dan menerima
 * response directory standar.
 */
public final class PlainEntryFixture {
    private PlainEntryFixture() {
    }

    public static Entry build(String baseDn) {
        Entry entry = new Entry(baseDn);
        entry.addAttribute("objectClass", "top");
        entry.addAttribute("objectClass", "organizationalUnit");
        entry.addAttribute("ou", "lab");
        entry.addAttribute("description", "LAB_PLAIN_ENTRY");
        return entry;
    }
}