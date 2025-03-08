package com.BridgeLabz.AddressBook.App.Repository;



import com.BridgeLabz.AddressBook.App.Entity.AddressBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressBookRepository extends JpaRepository<AddressBook, Integer> {
}
