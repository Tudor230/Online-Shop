package org.endava.onlineshop.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.endava.onlineshop.exception.BadRequestException;
import org.endava.onlineshop.model.dto.profile.AddressResponseDto;
import org.endava.onlineshop.model.dto.profile.CreateAddressRequestDto;
import org.endava.onlineshop.model.dto.profile.ProfileResponseDto;
import org.endava.onlineshop.model.dto.profile.UpdateProfileRequestDto;
import org.endava.onlineshop.model.entities.Address;
import org.endava.onlineshop.model.entities.User;
import org.endava.onlineshop.repository.AddressRepository;
import org.endava.onlineshop.repository.UserRepository;
import org.endava.onlineshop.security.KeycloakAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

  private final UserRepository userRepository;
  private final AddressRepository addressRepository;
  private final KeycloakAdminService keycloakAdminService;

  public ProfileService(
      UserRepository userRepository,
      AddressRepository addressRepository,
      KeycloakAdminService keycloakAdminService) {
    this.userRepository = userRepository;
    this.addressRepository = addressRepository;
    this.keycloakAdminService = keycloakAdminService;
  }

  @Transactional(readOnly = true)
  public ProfileResponseDto getProfile(User user) {
    return toProfileResponseDto(user, findSortedAddresses(user.getId()));
  }

  @Transactional
  public ProfileResponseDto updateProfile(User user, UpdateProfileRequestDto request) {
    String trimmedFirstName = request.firstName().trim();
    String trimmedLastName = request.lastName().trim();

    keycloakAdminService.updateUserNames(user.getId(), trimmedFirstName, trimmedLastName);

    user.setFirstName(trimmedFirstName);
    user.setLastName(trimmedLastName);

    User savedUser = userRepository.save(user);
    return toProfileResponseDto(savedUser, findSortedAddresses(savedUser.getId()));
  }

  @Transactional
  public ProfileResponseDto createAddress(User user, CreateAddressRequestDto request) {
    Address address = new Address();
    address.setUserId(user.getId());
    address.setRecipientName(request.recipientName().trim());
    address.setPhoneNumber(trimToNull(request.phoneNumber()));
    address.setAddressLine1(request.addressLine1().trim());
    address.setAddressLine2(trimToNull(request.addressLine2()));
    address.setCity(request.city().trim());
    address.setState(request.state().trim());
    address.setPostalCode(request.postalCode().trim());
    address.setCountry(request.country().trim());

    addressRepository.save(address);

    return toProfileResponseDto(user, findSortedAddresses(user.getId()));
  }

  @Transactional
  public ProfileResponseDto setPrimaryShippingAddress(User user, UUID addressId) {
    ensureAddressBelongsToUser(user.getId(), addressId);

    user.setDefaultShippingAddressId(addressId);
    User savedUser = userRepository.save(user);
    return toProfileResponseDto(savedUser, findSortedAddresses(savedUser.getId()));
  }

  @Transactional
  public ProfileResponseDto setPrimaryBillingAddress(User user, UUID addressId) {
    ensureAddressBelongsToUser(user.getId(), addressId);

    user.setDefaultBillingAddressId(addressId);
    User savedUser = userRepository.save(user);
    return toProfileResponseDto(savedUser, findSortedAddresses(savedUser.getId()));
  }

  @Transactional
  public ProfileResponseDto deleteAddress(User user, UUID addressId) {
    ensureAddressBelongsToUser(user.getId(), addressId);

    if (addressId.equals(user.getDefaultShippingAddressId())
        || addressId.equals(user.getDefaultBillingAddressId())) {
      throw new BadRequestException("Primary addresses cannot be deleted");
    }

    addressRepository.deleteById(addressId);
    return toProfileResponseDto(user, findSortedAddresses(user.getId()));
  }

  private void ensureAddressBelongsToUser(UUID userId, UUID addressId) {
    boolean addressExistsForUser =
        addressRepository.findByIdAndUserId(addressId, userId).isPresent();
    if (!addressExistsForUser) {
      throw new BadRequestException("Address does not belong to the authenticated user");
    }
  }

  private List<Address> findSortedAddresses(UUID userId) {
    return addressRepository.findByUserId(userId).stream()
        .sorted(
            Comparator.comparing(
                Address::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  private ProfileResponseDto toProfileResponseDto(User user, List<Address> addresses) {
    return new ProfileResponseDto(
        user.getId(),
        user.getEmail(),
        user.getFirstName(),
        user.getLastName(),
        user.getDefaultShippingAddressId(),
        user.getDefaultBillingAddressId(),
        addresses.stream().map(this::toAddressResponseDto).toList());
  }

  private AddressResponseDto toAddressResponseDto(Address address) {
    return new AddressResponseDto(
        address.getId(),
        address.getRecipientName(),
        address.getPhoneNumber(),
        address.getAddressLine1(),
        address.getAddressLine2(),
        address.getCity(),
        address.getState(),
        address.getPostalCode(),
        address.getCountry(),
        address.getCreatedAt(),
        address.getUpdatedAt());
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmedValue = value.trim();
    return trimmedValue.isEmpty() ? null : trimmedValue;
  }
}
