package testBookRide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import dataAccess.DataAccess;
import domain.Booking;
import domain.Driver;
import domain.Ride;
import domain.Traveler;

public class BookRideMockWhiteTest {
	
	static DataAccess sut;
	
	protected MockedStatic<Persistence> persistenceMock;

	@Mock
	protected  EntityManagerFactory entityManagerFactory;
	@Mock
	protected  EntityManager db;
	@Mock
    protected  EntityTransaction  et;
	
	// Driver del RIDE
	String driverName = "testingDriver";
	String driverPass = "passwordDriver";
	Driver testingDriver = new Driver(driverName, driverPass);
	
	// Traveler del RIDE
	String travelerName = "Mikel";
	String travelerPass = "1234";
	Traveler testTraveler = new Traveler(travelerName, travelerPass);
	
	// RIDE
	Ride ride = new Ride("Donostia", "Barcelona", null, 2, 40, testingDriver);
	
	// Asientos y descuento del BOOKING
	int seats;
	double desk;

	@Before
    public  void init() {
        MockitoAnnotations.openMocks(this);
        persistenceMock = Mockito.mockStatic(Persistence.class);
		persistenceMock.when(() -> Persistence.createEntityManagerFactory(Mockito.any()))
        .thenReturn(entityManagerFactory);
        
        Mockito.doReturn(db).when(entityManagerFactory).createEntityManager();
		Mockito.doReturn(et).when(db).getTransaction();
	    sut=new DataAccess(db);
    }
	@After
    public  void tearDown() {
		persistenceMock.close();
    }
	
	@Test
	// Excepción en db.transaction.begin() (la BD no está disponible), devuelve FALSE
	public void test1() {
	    try {
	        int seats = 2;
	        double desk = 0;
	        testTraveler.setMoney(100);
	        
	        // Configurar mock: queremos que getResultList() => [ traveler ] (una lista con el traveler)
	        TypedQuery<Traveler> queryMock = Mockito.mock(TypedQuery.class);
	        Mockito.when(db.createQuery(Mockito.anyString(), Mockito.eq(Traveler.class))).thenReturn(queryMock);
	        
	        List<Traveler> travelers = new ArrayList<>();
	        travelers.add(testTraveler);
	        Mockito.when(queryMock.getResultList()).thenReturn(travelers);
	        
	        // Configurar mock: queremos que db.persist(booking) lance una excepción
	        Mockito.doThrow(new RuntimeException("Booking duplicado")).when(db).persist(Mockito.any(Booking.class));

	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);

	        assertFalse(result);

	        // Verificar que se hacer rollback en la excepción
	        Mockito.verify(et).rollback();

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        sut.close();
	    }
	}
	
	@Test
	// Traveler no existe en la BD, devuelve FALSE
	public void test2() {
	    try {
	        seats = 1;
	        desk = 0;

	        // Configurar mock: queremos que getResultList() => [] (una lista vacía, y en consecuencia, null)
	        TypedQuery<Traveler> queryMock = Mockito.mock(TypedQuery.class);
	        
	        // Cuando se haga un createQuery de cualquier string, y contenga Traveler, se devuelve el Mock
	        Mockito.when(db.createQuery(Mockito.anyString(), Mockito.eq(Traveler.class))).thenReturn(queryMock);
	        
	        // Marcamos que devuelva nada
	        Mockito.when(queryMock.getResultList()).thenReturn(Collections.emptyList());
	               
	        // Ejecutar método
	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);
	        
	        assertFalse(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        sut.close();
	    }
	}
	
	@Test
	// Traveler si existe en la BD, pero el viaje no tiene sitio, devuelve FALSE
	public void test3() {
	    try {
	    	
	    	// Queremos reservar 3 sitios, pero solo hay 2 disponibles
	        int seats = 3;
	        double desk = 0;

	        // Configurar mock: queremos que getResultList() => [ traveler ] (una lista con el traveler)
	        TypedQuery<Traveler> queryMock = Mockito.mock(TypedQuery.class);

	        Mockito.when(db.createQuery(Mockito.anyString(), Mockito.eq(Traveler.class))).thenReturn(queryMock);

	        List<Traveler> travelers = new ArrayList<>();
	        travelers.add(testTraveler);

	        Mockito.when(queryMock.getResultList()).thenReturn(travelers);

	        // Ejecutar método
	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);

	        assertFalse(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        sut.close();
	    }
	}
	
	@Test
	// Traveler si existe en la BD, el viaje tiene sitio, pero no tiene suficiente dinero, devuelve FALSE
	public void test4() {
	    try {
	    	
	    	// Queremos reservar 2 sitios, y hay 2 disponibles
	        int seats = 2;
	        double desk = 0;
	        
	        // Le damos 50 euros, pero no son suficientes para pagar el viaje de 80
	        testTraveler.setMoney(50);

	        // Configurar mock: queremos que getResultList() => [ traveler ] (una lista con el traveler)
	        TypedQuery<Traveler> queryMock = Mockito.mock(TypedQuery.class);

	        Mockito.when(db.createQuery(Mockito.anyString(), Mockito.eq(Traveler.class))).thenReturn(queryMock);

	        List<Traveler> travelers = new ArrayList<>();
	        travelers.add(testTraveler);

	        Mockito.when(queryMock.getResultList()).thenReturn(travelers);

	        // Ejecutar método
	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);

	        assertFalse(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        sut.close();
	    }
	}
	
	@Test
	// Traveler si existe en la BD, el viaje tiene sitio, tiene suficiente dinero, el viaje se reserva, devuelve TRUE
	public void test5() {
	    try {
	    	// Queremos reservar 2 sitios, y hay 2 disponibles
	        int seats = 2;
	        double desk = 0;
	        
	        // Le damos 100 euros, son suficientes para pagar el viaje de 80
	        testTraveler.setMoney(100);

	        // Configurar mock: queremos que getResultList() => [ traveler ] (una lista con el traveler)
	        TypedQuery<Traveler> queryMock = Mockito.mock(TypedQuery.class);

	        Mockito.when(db.createQuery(Mockito.anyString(), Mockito.eq(Traveler.class))).thenReturn(queryMock);

	        List<Traveler> travelers = new ArrayList<>();
	        travelers.add(testTraveler);

	        Mockito.when(queryMock.getResultList()).thenReturn(travelers);

	        // Ejecutar método
	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);

	        assertTrue(result);
	        
	        // Verificación de los argumentos capturados
			ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class); // Captura objetos de tipo booking
			
			verify(db, times(1)).persist(bookingCaptor.capture()); // Verifica que db.persist() se ha llamado 1 vez
			
			// En el db.persist(booking) que se hace, se captura el "booking"
			Booking storedBooking = bookingCaptor.getValue();
			
			assertEquals(ride, storedBooking.getRide());
			assertEquals(testTraveler, storedBooking.getTraveler());
			assertEquals(seats, storedBooking.getSeats());
			assertEquals(desk, storedBooking.getDeskontua(), 0.001);
			
			verify(db).merge(ride); // Verifica que se ha llamado a db.merge(ride)
			verify(db).merge(testTraveler); // Verifica que se ha llamado a db.merge(testTraveler)
			verify(et).commit(); // Verifica que se ha llamado a et.commit()

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        sut.close();
	    }
	}
}
