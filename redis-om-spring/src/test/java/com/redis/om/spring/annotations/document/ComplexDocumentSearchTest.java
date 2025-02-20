package com.redis.om.spring.annotations.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;

import com.google.common.collect.Lists;
import com.redis.om.spring.AbstractBaseDocumentTest;
import com.redis.om.spring.annotations.document.fixtures.Address;
import com.redis.om.spring.annotations.document.fixtures.Attribute;
import com.redis.om.spring.annotations.document.fixtures.Order;
import com.redis.om.spring.annotations.document.fixtures.Permit;
import com.redis.om.spring.annotations.document.fixtures.PermitRepository;

public class ComplexDocumentSearchTest extends AbstractBaseDocumentTest {
  Permit permit1;
  Permit permit2;
  Permit permit3;

  @Autowired
  PermitRepository repository;

  @BeforeEach
  public void setup() {
    repository.deleteAll();

    // # Document 1
    Address address1 = Address.of("Lisbon", "25 de Abril");
    Order order1 = Order.of("O11", 1.5);
    Order order2 = Order.of("O12", 5.6);
    Attribute attribute11 = Attribute.of("size","S", Lists.newArrayList(order1));
    Attribute attribute12 = Attribute.of("size","M", Lists.newArrayList(order2));
    List<Attribute> attrList1 = Lists.newArrayList(attribute11, attribute12);
    permit1 = Permit.of( //
            address1, //
            "To construct a single detached house with a front covered veranda.", //
            "single detached house", //
            Set.of("demolition", "reconstruction"), //
            42000L, //
            new Point(38.7635877,-9.2018309), //
            List.of("started", "in_progress", "approved"), //
            attrList1
    );

    // # Document 2
    Address address2 = Address.of("Porto", "Av. da Liberdade");
    Order order21 = Order.of("O21", 1.2);
    Order order22 = Order.of("O22", 5.6);
    Attribute attribute21 = Attribute.of("color","red", Lists.newArrayList(order21));
    Attribute attribute22 = Attribute.of("color","blue", Lists.newArrayList(order22));
    List<Attribute> attrList2 = Lists.newArrayList(attribute21, attribute22);
    permit2 = Permit.of( //
            address2, //
            "To construct a loft", //
            "apartment", //
            Set.of("construction"), //
            53000L, //
            new Point(38.7205373,-9.148091), //
            List.of("started", "in_progress", "rejected"), //
            attrList2
    );

    // # Document 3
    Address address3 = Address.of("Lagos", "D. João");
    Order order31 = Order.of("ABC", 1.6);
    Order order32 = Order.of("DEF", 1.3);
    Order order33 = Order.of("GHJ", 1.6);
    Order order34 = Order.of("VBN", 1.0);
    Attribute attribute31 = Attribute.of("brand","A", Lists.newArrayList(order31, order33));
    Attribute attribute32 = Attribute.of("brand","B", Lists.newArrayList(order32, order34));
    List<Attribute> attrList3 = Lists.newArrayList(attribute31, attribute32);
    permit3 = Permit.of( //
            address3, //
            "New house build", //
            "house", //
            Set.of("construction", "design"), //
            260000L, //
            new Point(37.0990749,-8.6868258), //
            List.of("started", "in_progress", "postponed"), //
            attrList3
    );

    repository.saveAll(List.of(permit1, permit2, permit3));
  }

  @Test
  void testFindbyConstructionValue() {
    long value = 42000L;
    Iterable<Permit> permits = repository.findByConstructionValue(value);
    assertThat(permits).containsExactly(permit1);
  }

  @Test
  void testFullTextSearch() {
    String q = "veranda";
    Iterable<Permit> permits = repository.search(q);
    assertThat(permits).containsExactly(permit1);
  }

  @Test
  void testByBuildingType() {
    String type = "detached";
    Iterable<Permit> permits =  repository.findByBuildingType(type);
    assertThat(permits).containsExactly(permit1);
  }

  @Test
  void testByCity() {
    Iterable<Permit> permits =  repository.findByAddress_City("Lisbon");
    assertThat(permits).containsExactly(permit1);

    permits =  repository.findByAddress_City("Porto");
    assertThat(permits).containsExactly(permit2);
  }

  @Test
  void testByTags() {
    Set<String> wts = Set.of("design","construction");
    Iterable<Permit> permits =  repository.findByWorkType(wts);
    assertThat(permits).containsExactlyInAnyOrder(permit2, permit3);
  }

  @Test
  void testByAllTags() {
    Set<String> wts = Set.of("design","construction");
    Iterable<Permit> permits =  repository.findByWorkTypeContainingAll(wts);
    assertThat(permits).containsExactly(permit3);
  }

  @Test
  void testByBuildingTypeAndDescription() {
    String buildingType = "house";
    String description = "new";
    Iterable<Permit> permits = repository.findByBuildingTypeAndDescription(buildingType, description);
    assertThat(permits).containsExactly(permit3);
  }

  @Test
  void testByCityOrDescription() {
    String city = "Lagos";
    String description = "detached";
    Iterable<Permit> permits = repository.findByAddress_CityOrDescription(city, description);
    assertThat(permits).containsExactlyInAnyOrder(permit1, permit3);
  }

  @Test
  void testFullTextSearchShouldNotEscapeSearchString() {
    String q = "Single detached House";
    Iterable<Permit> permits = repository.search(q);
    assertThat(permits).containsExactly(permit1);
  }
  
  @Test
  void testFullTextSearchWithExplicitEscaping() {
    String q = "house\\ with\\ a\\ front";
    Iterable<Permit> permits = repository.search(q);
    assertThat(permits).isEmpty();
  }
  
  @Test
  void testFullTextSearchExplicitEscapeSearchTerm() {
    String q = "\"single detached house\"";
    Iterable<Permit> permits = repository.search(q);
    assertThat(permits).containsExactly(permit1);
  }
  
  @Test
  void testFullTextSearchExplicitEscapeSearchTermPrefixSearch() {
    String q = "To construct*";
    Iterable<Permit> permits = repository.search(q);
    assertThat(permits).containsExactly(permit1,permit2);
  }
}
