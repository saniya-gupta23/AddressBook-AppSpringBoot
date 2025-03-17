package com.BridgeLabz.AddressBook.App.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordDTO {
    private String email;
    private String newPassword;
}
