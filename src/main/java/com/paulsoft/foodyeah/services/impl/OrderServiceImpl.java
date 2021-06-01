package com.paulsoft.foodyeah.services.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.paulsoft.foodyeah.dtos.OrderDetailDto.CreateOrderDetailDto;
import com.paulsoft.foodyeah.dtos.orderDto.CreateOrderDto;
import com.paulsoft.foodyeah.dtos.orderDto.OrderDto;
import com.paulsoft.foodyeah.entities.Customer;
import com.paulsoft.foodyeah.entities.Order;
import com.paulsoft.foodyeah.entities.OrderDetail;
import com.paulsoft.foodyeah.entities.Product;
import com.paulsoft.foodyeah.entities.*;
import com.paulsoft.foodyeah.exceptions.InternalServerErrorException;
import com.paulsoft.foodyeah.exceptions.NotFoundException;
import com.paulsoft.foodyeah.exceptions.ResourceException;
import com.paulsoft.foodyeah.repositories.*;
import com.paulsoft.foodyeah.services.OrderService;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CardRepository cardRepository;

    public static final ModelMapper modelMapper=new ModelMapper();

    @Override
    public List<OrderDto> getOrders() throws ResourceException {
        return convertToResources(orderRepository.findAll());
    }

    @Override
    public List<OrderDto> getOrdersByCustomerId(Long id) throws ResourceException {
        return convertToResources(orderRepository.findAllByCustomerId(id));
    }

    @Override
    public OrderDto getOrderById(Long id) throws ResourceException {
        return convertToResource(getOrderEntity(id));
    }

    @Override
    @Transactional
    public OrderDto createOrder(CreateOrderDto createOrderDto) throws ResourceException, ParseException {
        Customer customer = customerRepository.findById(createOrderDto.getCustomerId())
                .orElseThrow(() -> new NotFoundException("NOT_FOUND", "NOT_FOUND"));
        Order order = new Order();
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        Date date = formatter.parse(formatter.format(new Date()));
        order.setDate(date);
        order.setTotalPrice(0d);
        order.setCustomer(customer);
        orderRepository.save(order);
        Double total = 0d;
        List<CreateOrderDetailDto> orderDetails = createOrderDto.getDetails();
        for (CreateOrderDetailDto orderDetail : orderDetails) {
            OrderDetail orderDetailEntity = new OrderDetail();
            Product product = productRepository.findById(orderDetail.getProductId())
                    .orElseThrow(() -> new NotFoundException("NOT_FOUND", "NOT_FOUND"));
            orderDetailEntity.setOrder(order);
            orderDetailEntity.setProduct(product);
            orderDetailEntity.setUnitName(product.getName());
            orderDetailEntity.setUnitPrice(product.getProductPrice());
            orderDetailEntity.setQuantity(orderDetail.getQuantity());
            orderDetailEntity.setTotalPrice(product.getProductPrice() * orderDetail.getQuantity());
            total += orderDetailEntity.getTotalPrice();
            orderDetailRepository.save(orderDetailEntity);
        }
        order.setTotalPrice(total);
        return convertToResource(orderRepository.save(order));
    }

    @Transactional
    public void decreaseMoney(Long orderId, Long cardId) throws ResourceException{
        Card card = cardRepository.findById(cardId)
                .orElseThrow(()->new NotFoundException("NOT_FOUND","NOT_FOUND"));
        Order order = orderRepository.findById(orderId)
                .orElseThrow(()->new NotFoundException("NOT_FOUND","NOT_FOUND"));
        if(order.getTotalPrice() > card.getCardMoney()) {
            throw new InternalServerErrorException("NOT ENOUGH MONEY", "NOT ENOUGH MONEY");
        }
        card.setCardMoney(card.getCardMoney()-order.getTotalPrice());
        cardRepository.save(card);
    }

    private Order getOrderEntity(Long id) throws ResourceException {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("NOT_FOUND","NOT_FOUND"));
    }

    private List<OrderDto> convertToResources(List<Order> orders) {
        return orders.stream().map(x -> modelMapper.map(x, OrderDto.class)).collect(Collectors.toList());
    }

    private Order convertToEntity(CreateOrderDto resource) {
        return modelMapper.map(resource, Order.class);
    }

    private OrderDto convertToResource(Order entity) {
        return modelMapper.map(entity, OrderDto.class);
    }

}
