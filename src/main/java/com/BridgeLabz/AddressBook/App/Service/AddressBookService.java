package com.BridgeLabz.AddressBook.App.Service;

import com.BridgeLabz.AddressBook.App.DTO.AddressBookDTO;
import com.BridgeLabz.AddressBook.App.Entity.AddressBook;
import com.BridgeLabz.AddressBook.App.Interfaces.IAddressBookService;
import com.BridgeLabz.AddressBook.App.Repository.AddressBookRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import com.BridgeLabz.AddressBook.App.Interfaces.IAddressBookService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
<<<<<<< HEAD
=======
@Slf4j
>>>>>>> UC8_RepositoryLayer_SaveToSQL
public class AddressBookService implements IAddressBookService {

    @Autowired
    private AddressBookRepository addressBookRepository;

    @Override
    public List<AddressBook> getAllContacts() {
        return addressBookRepository.findAll();
    }

    @Override
    public Optional<AddressBook> getContactById(int id) {
        return addressBookRepository.findById(id);
    }

    @Override
    public AddressBook addContact(AddressBookDTO addressBookDTO) {
        AddressBook contact = new AddressBook();
        contact.setId(idCounter++);
        contact.setName(addressBookDTO.getName());
        contact.setPhone(addressBookDTO.getPhone());
        contact.setAddress(addressBookDTO.getAddress());
        return addressBookRepository.save(contact);
    }

    @Override
    public Optional<AddressBook> updateContact(int id, AddressBookDTO addressBookDTO) {
        return addressBookRepository.findById(id).map(contact -> {
            contact.setName(addressBookDTO.getName());
            contact.setPhone(addressBookDTO.getPhone());
            contact.setAddress(addressBookDTO.getAddress());
            return addressBookRepository.save(contact);
        });
    }

    @Override
    public boolean deleteContact(int id) {
        if (addressBookRepository.existsById(id)) {
            addressBookRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
