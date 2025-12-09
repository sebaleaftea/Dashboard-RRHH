import Container from 'react-bootstrap/Container';
import Nav from 'react-bootstrap/Nav';
import Navbar from 'react-bootstrap/Navbar';
import NavDropdown from 'react-bootstrap/NavDropdown';

// ...existing code...
export default function NavBar({ onMenuClick }) {
  return (
    <Navbar expand="lg" bg="dark" data-bs-theme="dark" className="bg-body-tertiary border-bottom px-3">
      <Container>
        {/* Mobile sidebar toggle */}
        <button className="btn btn-outline-primary d-lg-none me-2" onClick={onMenuClick}>
          <i className="bi bi-list" /> Menu
        </button>

        <Navbar.Brand href="">Tiendas Mass Chile</Navbar.Brand>
        <Navbar.Toggle aria-controls="basic-navbar-nav" />
        <Navbar.Collapse id="basic-navbar-nav">
          <Nav className="me-auto">
            <Nav.Link href="#home">Home</Nav.Link>
            <Nav.Link href="#link">Anexo</Nav.Link>
            <NavDropdown title="Dropdown" id="basic-nav-dropdown">
              <NavDropdown.Item href="#action/3.1">Item A</NavDropdown.Item>
              <NavDropdown.Item href="#action/3.2">Item B</NavDropdown.Item>
              <NavDropdown.Item href="#action/3.3">Item C</NavDropdown.Item>
              <NavDropdown.Divider />
              <NavDropdown.Item href="#action/3.4">Link solito por que es mas bonito</NavDropdown.Item>
            </NavDropdown>
          </Nav>
        </Navbar.Collapse>
      </Container>
    </Navbar>
  );
}