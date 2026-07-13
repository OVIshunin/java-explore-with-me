package ru.practicum.ewm.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewUserRequest {

    @NotBlank(message = "email cannot be blank")
    @Email(message = "email should be valid")
    @Size(min = 6, max = 254, message = "email size must be between 6 and 254")
    private String email;

    @NotBlank(message = "name cannot be blank")
    @Size(min = 2, max = 250, message = "name size must be between 2 and 250")
    private String name;
}