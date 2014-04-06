package edu.sjsu.cmpe.procurement.domain;

public class BookDetails {

	private Book[] shipped_books = new Book[20];

	public Book[] getShipped_books() {
		return shipped_books;
	}

	public void setShipped_books(Book[] shipped_books) {
		this.shipped_books = shipped_books;
	}
}
