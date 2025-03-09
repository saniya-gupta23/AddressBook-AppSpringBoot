package com.BridgeLabz.AddressBook.App.Interfaces;

import com.BridgeLabz.AddressBook.App.DTO.AddressBookDTO;
import com.BridgeLabz.AddressBook.App.Entity.AddressBook;
import java.util.List;
import java.util.Optional;

public interface IAddressBookService {
    List<AddressBook> getAllContacts();
    Optional<AddressBook> getContactById(int id);
    AddressBook addContact(AddressBookDTO addressBookDTO);
    Optional<AddressBook> updateContact(int id, AddressBookDTO addressBookDTO);
    boolean deleteContact(int id);
}
