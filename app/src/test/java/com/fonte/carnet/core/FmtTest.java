package com.fonte.carnet.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FmtTest {

    @Test
    public void kg() {
        assertEquals("100", Fmt.kg(100));
        assertEquals("102,5", Fmt.kg(102.5));
        assertEquals("101,25", Fmt.kg(101.25));
        assertEquals("0,05", Fmt.kg(0.05));
        assertEquals("0", Fmt.kg(0));
        assertEquals("-2,5", Fmt.kg(-2.5));
    }

    @Test
    public void tonnage() {
        assertEquals("850 kg", Fmt.tonnage(850));
        assertEquals("4,2 t", Fmt.tonnage(4180));
        assertEquals("12 t", Fmt.tonnage(12020));
    }

    @Test
    public void parse() {
        assertEquals(102.5, Fmt.parse("102,5"), 1e-9);
        assertEquals(102.5, Fmt.parse(" 102.5 "), 1e-9);
        assertTrue(Double.isNaN(Fmt.parse("")));
        assertTrue(Double.isNaN(Fmt.parse("abc")));
        assertEquals(8, Fmt.parseInt("8"));
        assertEquals(0, Fmt.parseInt("-3"));
        assertEquals(0, Fmt.parseInt(""));
    }

    @Test
    public void times() {
        assertEquals("42 min", Fmt.duration(42 * 60_000L));
        assertEquals("1 h 05", Fmt.duration(65 * 60_000L));
        assertEquals("1:05", Fmt.clock(65_000));
        assertEquals("1:02:09", Fmt.clock(3729_000));
        assertEquals("3 séries", Fmt.plural(3, "série"));
        assertEquals("1 série", Fmt.plural(1, "série"));
    }
}
