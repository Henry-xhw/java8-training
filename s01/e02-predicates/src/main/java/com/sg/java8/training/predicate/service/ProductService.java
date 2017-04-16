package com.sg.java8.training.predicate.service;

import com.sg.java8.training.model.Manager;
import com.sg.java8.training.model.Product;
import com.sg.java8.training.model.Section;
import com.sg.java8.training.model.Store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple service for managing {@link Product} entities
 *
 * @author bogdan.solga
 */
public class ProductService {

    private static Store STORE;

    static {
        final Section tabletsSection = new Section(1, "Tablets", buildDefaultTablets());
        final Section monitorsSection = new Section(1, "Monitors", buildDefaultMonitors());

        final Manager john = new Manager(1, "John Doe");
        final Manager jane = new Manager(2, "Jane Charming");

        STORE = new Store(1, "eMag", "Over there",
                new HashSet<>(Arrays.asList(tabletsSection, monitorsSection)),
                new HashSet<>(Arrays.asList(john, jane)));
    }

    public List<Product> getNexusTablets() {
        final Section tablets = STORE.getStoreSections()
                                     .stream()
                                     .filter(section -> section.getSectionName().equals("Tablets"))
                                     .findFirst()
                                     .orElseThrow(() -> new IllegalArgumentException("There's no section named 'Tablets'"));

        return tablets.getProducts()
                      .stream()
                      .filter(product -> product.getName().toLowerCase().contains("nexus"))
                      .collect(Collectors.toList());
    }

    private static List<Product> buildDefaultTablets() {
        final List<Product> tablets = new ArrayList<>();

        tablets.add(new Product(1, "Google Nexus 7 2013", 200));
        tablets.add(new Product(2, "Apple Ipad Pro 9.7", 300));
        tablets.add(new Product(3, "Samsung Galaxy Tab S2", 350));
        tablets.add(new Product(4, "Microsoft Surface Pro 4", 400));

        return tablets;
    }

    private static List<Product> buildDefaultMonitors() {
        final List<Product> monitors = new ArrayList<>();

        monitors.add(new Product(5, "Samsung CF791", 500));
        monitors.add(new Product(6, "LG 32UD99", 550));
        monitors.add(new Product(6, "Samsung CH711", 600));

        return monitors;
    }
}
