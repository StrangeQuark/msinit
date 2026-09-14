// Integration file: Auth

package com.example.vaultservice.utility;

import com.example.vaultservice.serviceuser.ServiceUserRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class RoleEncryptDecryptConverter implements AttributeConverter<ServiceUserRole, String> {

    @Override
    public String convertToDatabaseColumn(ServiceUserRole role) {
        if (role == null) return null;
        return EncryptionUtility.encrypt(role.name());
    }

    @Override
    public ServiceUserRole convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        String decrypted = EncryptionUtility.decrypt(dbData);
        return ServiceUserRole.valueOf(decrypted);
    }
}
