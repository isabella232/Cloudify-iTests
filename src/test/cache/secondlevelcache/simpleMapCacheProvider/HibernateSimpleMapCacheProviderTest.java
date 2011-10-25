package test.cache.secondlevelcache.simpleMapCacheProvider;

import com.j_spaces.map.Envelope;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.openspaces.admin.machine.Machine;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import test.AbstractTest;
import test.data.Person;
import test.utils.DBUtils;
import test.utils.ToStringUtils;

import java.io.IOException;

import static test.utils.LogUtils.log;

public class HibernateSimpleMapCacheProviderTest extends AbstractTest {

    private Machine machine;
    private int dbPort;
    private SessionFactory sessionFactory;
    private int OBJECTS = 10;
    private int hsqldbProcessId;

    @BeforeMethod
    public void setup() throws IOException, InterruptedException {
        assertTrue(admin.getMachines().waitFor(1));
        assertTrue(admin.getGridServiceAgents().waitFor(1));

        Machine[] machines = admin.getMachines().getMachines();
        machine = machines[0];

        dbPort = 11158;

        System.setProperty("port", String.valueOf(dbPort));
        System.setProperty("host", machine.getHostAddress());
        System.setProperty("group", admin.getGroups()[0]);

        log("load HSQL DB on machine - " + ToStringUtils.machineToString(machine));
        hsqldbProcessId = DBUtils.loadHSQLDB(machine, "simpleMapCacheProvider", dbPort);

        String location = "/test/cache/secondlevelcache/resources/simple-map-hibernate-applicationcontext.xml";
        ApplicationContext ctx = new ClassPathXmlApplicationContext(location);
        sessionFactory = (SessionFactory) ctx.getBean("sessionFactory");
    }

    @Test(timeOut = DEFAULT_TEST_TIMEOUT, groups = "1")
    public void testSimpleMapCacheProvider() throws Exception {
        Session saveSession = sessionFactory.openSession();
        Transaction txn = saveSession.beginTransaction();
        for (int i = 0; i < OBJECTS; i++) {
            saveSession.save(new Person(Long.valueOf(i), "name" + i));
        }
        txn.commit();
        saveSession.close();

        Thread.sleep(5000);

        Session getSession = sessionFactory.openSession();
        Transaction txn1 = getSession.beginTransaction();
        for (int i = 0; i < OBJECTS; i++) {
            assertNotNull(getSession.get(Person.class, Long.valueOf(i)));
        }
        txn1.commit();
        getSession.close();

        UrlSpaceConfigurer configurer = new UrlSpaceConfigurer("jini://*/*/slcacheHibernateSpace");
        configurer.lookupGroups(admin.getGroups()[0]);
        GigaSpace gigaSpace = new GigaSpaceConfigurer(configurer.space()).gigaSpace();

        Assert.assertEquals(OBJECTS, gigaSpace.count(new Envelope()));

        Session updateSession = sessionFactory.openSession();
        Transaction txn2 = updateSession.beginTransaction();
        for (int i = 0; i < OBJECTS; i++) {
            updateSession.saveOrUpdate(new Person(Long.valueOf(i), "updated name" + i));
        }
        txn2.commit();
        updateSession.close();

        getSession = sessionFactory.openSession();
        Transaction txn3 = getSession.beginTransaction();
        Person p, p1;
        for (int i = 0; i < OBJECTS; i++) {
            p = (Person) getSession.get(Person.class, Long.valueOf(i));
            p1 = (Person) getSession.get(Person.class, Long.valueOf(i));
            assertNotNull(p);
            assertNotNull(p1);
            Assert.assertEquals("updated name" + i, p.getName());
            Assert.assertEquals("updated name" + i, p1.getName());
        }
        txn3.commit();
        getSession.close();

        gigaSpace.clear(null);
        Session session = sessionFactory.openSession();
        Transaction txn4 = session.beginTransaction();
        session.clear();
        session.evict(Person.class);
        session.flush();
        txn4.commit();

        Assert.assertEquals(0, gigaSpace.count(new Envelope()));
    }

    @Override
    @AfterMethod
    public void afterTest() {
        machine.getGridServiceAgent().killByAgentId(hsqldbProcessId);
        super.afterTest();
    }
}
