package ru.practicum.mapper;

import ru.practicum.dto.UserDto;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserShortDto;
import ru.practicum.model.User;

public class UserMapper {

    public static UserDto toUserDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        return userDto;
    }

    public static UserShortDto toUserShortDto(User user) {
        if (user == null) {
            return null;
        }
        UserShortDto userShortDto = new UserShortDto();
        userShortDto.setId(user.getId());
        userShortDto.setName(user.getName());
        return userShortDto;
    }

    public static User toUser(NewUserRequest newUserRequest) {
        if (newUserRequest == null) {
            return null;
        }
        User user = new User();
        user.setName(newUserRequest.getName());
        user.setEmail(newUserRequest.getEmail());
        return user;
    }

    public static User toUser(UserDto userDto) {
        if (userDto == null) {
            return null;
        }
        User user = new User();
        user.setId(userDto.getId());
        user.setName(userDto.getName());
        user.setEmail(userDto.getEmail());
        return user;
    }
}
