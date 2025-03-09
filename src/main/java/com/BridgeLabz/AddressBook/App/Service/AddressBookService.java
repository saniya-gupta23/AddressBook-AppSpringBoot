package com.BridgeLabz.AddressBook.App.Service;

import com.BridgeLabz.AddressBook.App.Entity.AddressBook;
import com.BridgeLabz.AddressBook.App.DTO.AddressBookDTO;
import com.BridgeLabz.AddressBook.App.Exception.AddressBookException;
import com.BridgeLabz.AddressBook.App.Repository.AddressBookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.BridgeLabz.AddressBook.App.Interfaces.IAddressBookService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressBookService implements IAddressBookService {

    @Autowired
    private AddressBookRepository addressBookRepository;

    public List<AddressBook> getAllContacts() {
        log.info("Fetching all contacts from the database");
        return addressBookRepository.findAll();
    }

    public Optional<AddressBook> getContactById(int id) {
        log.info("Fetching contact with ID: {}", id);
        return addressBookRepository.findById(id)
                .or(() -> {
                    log.error("Contact with ID {} not found", id);
                    throw new AddressBookException("Contact with ID " + id + " not found");
                });
    }

    public AddressBook addContact(AddressBookDTO addressBookDTO) {
        log.info("Adding new contact: {}", addressBookDTO);
        AddressBook contact = new AddressBook();
        contact.setId(idCounter++);
        contact.setName(addressBookDTO.getName());
        contact.setPhone(addressBookDTO.getPhone());
        contact.setAddress(addressBookDTO.getAddress());

        AddressBook savedContact = addressBookRepository.save(contact); // Save to DB
        log.debug("Contact added successfully: {}", savedContact);
        return savedContact;
    }

    public Optional<AddressBook> updateContact(int id, AddressBookDTO addressBookDTO) {
        log.info("Updating contact with ID: {}", id);
        return addressBookRepository.findById(id).map(existingContact -> {
            existingContact.setName(addressBookDTO.getName());
            existingContact.setPhone(addressBookDTO.getPhone());
            existingContact.setAddress(addressBookDTO.getAddress());

            AddressBook updatedContact = addressBookRepository.save(existingContact); // Save updated data
            log.debug("Updated contact: {}", updatedContact);
            return updatedContact;
        });
    }

    public boolean deleteContact(int id) {
        log.warn("Deleting contact with ID: {}", id);
        if (addressBookRepository.existsById(id)) {
            addressBookRepository.deleteById(id);
            log.info("Contact with ID {} deleted successfully", id);
            return true;
        } else {
            log.error("Contact with ID {} not found, cannot delete", id);
            return false;
        }
    }
}
