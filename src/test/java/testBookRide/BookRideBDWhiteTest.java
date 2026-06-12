package testBookRide;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.TypedQuery;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import dataAccess.DataAccess;
import domain.Booking;
import domain.Driver;
import domain.Ride;
import domain.Traveler;
import testOperations.TestDataAccess;

public class BookRideBDWhiteTest {

	//sut:system under test
	static DataAccess sut = new DataAccess();
	 
	//additional operations needed to execute the test 
	static TestDataAccess testDA = new TestDataAccess();

	// Driver del RIDE
	String driverName = "testingDriver";
	String driverPass = "passwordDriver";
	Driver testingDriver = new Driver(driverName, driverPass);
	
	// Traveler del RIDE
	String travelerName = "Mikel";
	String travelerPass = "1234";
	double travelerMoney;
	
	// RIDE (con 2 plazas, y precio de 40€)
	String rideFrom = "Donostia";
	String rideTo = "Barcelona";
	Ride ride = new Ride("Donostia", "Barcelona", null, 2, 40, testingDriver);
	
	// Asientos y Descuento del BOOKING
	int seats;
	double desk;
	
	@Before
	public void setUp() {
	    testDA.open();
	    testDA.removeBooking(travelerName);
	    while (testDA.removeRide(driverName, rideFrom, rideTo, null) != null) {
	        // sigue borrando hasta que no quede ninguno
	    }
	    testDA.removeTraveler(travelerName);
	    testDA.close();
	}
	
	@Test
	// Se lanza una excepción en la ejecución de bookRide(), devuelve FALSE
	public void test1() {
	    try {
	        seats = 2;
	        desk = 0;
	        travelerMoney = 100;

	        testDA.open();
	        testDA.createTravelerWithMoney(travelerName, travelerPass, travelerMoney);
	        testDA.close();

	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);
	        sut.close();

	        assertFalse(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        testDA.open();
	        testDA.removeTraveler(travelerName);
	        testDA.close();
	    }
	}
	
	@Test
	// Traveler no existe en la BD, devuelve FALSE
	public void test2() {
	    try {
	        seats = 1;
	        desk = 0;

	        // NO SE HA INSERTADO NADA CON EL testDA
	        
	        sut.open();

	        // Intentar bookride sin haber populado la base de datos
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);

	        sut.close();

	        assertFalse(result);

	    } catch (Exception e) {
	    	e.printStackTrace();
	        sut.close();
	        fail();
	    }
	}
	
	@Test
	// Traveler existe, pero no hay plazas suficientes, devuelve FALSE
	public void test3() {
	    try {
	        seats = 3; // pedimos más de los que hay
	        desk = 0;
	        travelerMoney = 5;
	        
	        // INSERTAMOS Traveler 
	        testDA.open();
	        testDA.createTravelerWithMoney(travelerName, travelerPass, travelerMoney);
	        testDA.close();
	        
	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);
	        sut.close();

	        assertFalse(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        testDA.open();
	        testDA.removeTraveler(travelerName);
	        testDA.close();
	    }
	}
	

	@Test
	// Traveler si existe en la BD, el viaje tiene sitio, pero no tiene suficiente dinero, devuelve FALSE
	public void test4() {
	    try {
	    	// Queremos reservar 2 sitios, y hay 2 disponibles
	        seats = 2;
	        desk = 0;
	        travelerMoney = 5; // Le asignamos dinero, pero no es suficiente
	        
	        testDA.open();
	        testDA.createTravelerWithMoney(travelerName, travelerPass, travelerMoney);
	        testDA.close();

	        // Ejecutar método
	        sut.open();
	        boolean result = sut.bookRide(travelerName, ride, seats, desk);
	        sut.close();
	        
	        assertFalse(result);

	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        testDA.open();
	        testDA.removeTraveler(travelerName);
	        testDA.close();
	    }
	}

	
	@Test
	// Traveler si existe en la BD, el viaje tiene sitio, tiene suficiente dinero, el viaje se reserva, devuelve TRUE
	public void test5() {
	    try {
	    	// Queremos reservar 2 sitios, y hay 2 disponibles
	        seats = 2;
	        desk = 0;
	        travelerMoney = 100;
	        
	        testDA.open();
	        testDA.createTravelerWithMoney(travelerName, travelerPass, travelerMoney);
	        testDA.addDriverWithRide(driverName, rideFrom, rideTo, null, 2, 40);
	        testDA.close();
	        
	        // Ejecutar método
	        sut.open();
	        List<Ride> rides = sut.getRidesByDriver(driverName);
	        Ride persistedRide = rides.get(0);
	        boolean result = sut.bookRide(travelerName, persistedRide, seats, desk);
	        sut.close();
	        
	        assertTrue(result);
	        
	    } catch (Exception e) {
	        e.printStackTrace();
	        fail();
	    } finally {
	        testDA.open();
	        testDA.removeBooking(travelerName);
	        Ride r;
	        do {
	            r = testDA.removeRide(driverName, rideFrom, rideTo, null);
	        } while (r != null);
	        testDA.removeTraveler(travelerName);
	        testDA.close();
	    }
	}
	
	
	
}
